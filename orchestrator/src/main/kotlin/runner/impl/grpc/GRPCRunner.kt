package technology.idlab.runner.impl.grpc

import com.google.protobuf.ByteString
import io.grpc.ConnectivityState
import io.grpc.StatusException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import rdfc.ChannelOuterClass.ChannelMessage
import rdfc.ChannelOuterClass.ChannelMessageType
import rdfc.RunnerGrpcKt.RunnerCoroutineStub
import rdfc.channel
import rdfc.channelData
import rdfc.channelMessage
import rdfc.dataOrNull
import runner.impl.grpc.Config
import technology.idlab.ConnectionException
import technology.idlab.UnrecognizedRequestException
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.Runner
import technology.idlab.util.Log
import technology.idlab.util.retries

/**
 * This runner has GRPC built-in, so the only configuration that an extending class needs to provide
 * is the host and port of the GRPC server, as well as actually booting the process.
 */
abstract class GRPCRunner(
    /** Location of the gRPC server. */
    config: Config,
    /** Stages to execute. */
    stages: Collection<IRStage>
) : Runner(stages) {
  // The URI of this runner.
  override val uri: String = config.uri

  // Connect to the server at initialisation.
  private val conn = config.connect()

  // Create a gRPC stub.
  private val grpc = RunnerCoroutineStub(conn)

  // Incoming messages.
  private val messages = Channel<ChannelMessage>()

  init {
    Log.shared.debug { "Waiting for connection." }

    // Attempt to initialize a connection.
    runBlocking {
      retries(5) {
        if (conn.getState(true) != ConnectivityState.READY) {
          throw Exception("gRPC connection not ready.")
        }
      }

      // Load all stages.
      for (stage in stages) {
        val payload = serialize(stage)
        try {
          grpc.load(payload)
        } catch (e: StatusException) {
          throw ConnectionException(e)
        }
      }
    }

    Log.shared.debug { "GRPCRunner is online." }
  }

  /**
   * This flow collector is used to route messages from the gRPC server to the main orchestrator. It
   * maps the incoming `ChannelMessage` to a payload, and sends it to the broker. Note that this
   * flow collector is not a coroutine, but a lambda that is called by the gRPC server.
   */
  private val collector =
      FlowCollector<ChannelMessage> {
        Log.shared.debug { it.channel.uri }
        if (it.type == ChannelMessageType.CLOSE) {
          broker.unregister(it.channel.uri)
          return@FlowCollector
        }

        if (it.type == ChannelMessageType.UNRECOGNIZED) {
          throw UnrecognizedRequestException()
        }

        // We can now assume that the message is of type DATA.
        assert(it.type == ChannelMessageType.DATA)

        // If no data was received, log an error and substitute an empty byte array.
        val data =
            it.dataOrNull?.bytes?.toByteArray()
                ?: run {
                  Log.shared.severe("Channel '${it.channel.uri}' received a message with no data.")
                  return@run "".toByteArray()
                }

        // Send message to the broker.
        broker.send(it.channel.uri, data)
      }

  override suspend fun exit() {
    conn.shutdown()
  }

  /** Parse the incoming URI and bytes as a ChannelMessageKT, and send it to the gRPC flow. */
  override fun receiveBrokerMessage(uri: String, data: ByteArray) {
    val message = channelMessage {
      this.channel = channel { this.uri = uri }
      this.type = ChannelMessageType.DATA
      this.data = channelData { this.bytes = ByteString.copyFrom(data) }
    }

    scheduleTask { messages.send(message) }
  }

  /** Close a channel by sending a close message to the gRPC server. */
  override fun closingBrokerChannel(uri: String) {
    val message = channelMessage {
      this.channel = channel { this.uri = uri }
      this.type = ChannelMessageType.CLOSE
    }

    scheduleTask { messages.send(message) }
  }

  /**
   * Execute the runner by routing messages from the broker to the gRPC server. This function will
   * block until the gRPC server has finished processing all messages.
   *
   * @throws ConnectionException If the gRPC server is not reachable.
   */
  override suspend fun exec() {
    // Attempt to execute the pipelines.
    try {
      Log.shared.debug("Begin routing messages in GRPCRunner.")
      grpc.exec(messages.receiveAsFlow()).collect(this@GRPCRunner.collector)
      Log.shared.debug("Ending routing messages in GRPCRunner.")
    } catch (e: StatusException) {
      throw ConnectionException(e)
    }
  }
}
