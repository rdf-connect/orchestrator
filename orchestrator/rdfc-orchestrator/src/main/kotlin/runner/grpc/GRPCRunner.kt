package technology.idlab.rdfc.orchestrator.runner.grpc

import com.google.protobuf.ByteString
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import java.io.File
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.runBlocking
import rdfc.ChannelOuterClass.ChannelMessage
import rdfc.ChannelOuterClass.ChannelMessageType
import rdfc.RunnerGrpcKt.RunnerCoroutineStub
import rdfc.channel
import rdfc.channelData
import rdfc.channelMessage
import technology.idlab.rdfc.core.extensions.rawPath
import technology.idlab.rdfc.core.process.ProcessManager
import technology.idlab.rdfc.core.util.retries
import technology.idlab.rdfc.intermediate.IRRunner
import technology.idlab.rdfc.intermediate.IRStage
import technology.idlab.rdfc.intermediate.runner.RunnerType
import technology.idlab.rdfc.orchestrator.runner.Runner
import technology.idlab.rdfc.orchestrator.runner.exception.UnrecognizedRequestException

/**
 * Attempt to connect to a gRPC server. `attempts` indicates the number of retries before throwing
 * an exception if the connection repeatedly fails.
 *
 * @param attempts The number of attempts to connect to the server.
 * @throws IllegalStateException If the connection is not ready after the given number of attempts.
 */
private suspend fun ManagedChannel.attemptConnection(attempts: Int) {
  retries(attempts) {
    check(this.getState(true) == ConnectivityState.READY) { "gRPC connection not ready." }
  }
}

/**
 * A runner which communicates with a remote process over gRPC to execute a set of stages.
 *
 * @param config The configuration object of the gRPC server.
 * @param stages The stages for which the runner is responsible.
 * @property conn A gRPC connection to the server. The current implementation uses a single
 *   connection and connects at initialization. There is no support for reconnecting.
 * @property grpc A gRPC stub to communicate with the server.
 * @property messages A channel to route messages from the gRPC server to the main orchestrator.
 * @constructor Create a new gRPC runner with the given configuration and stages. During
 *   initialization, the runner will connect to the gRPC server and load all stages.
 */
open class GRPCRunner(config: GRPCConfig, stages: Collection<IRStage>) : Runner(stages) {
  private val conn = config.connect()
  private val grpc = RunnerCoroutineStub(conn)
  private val messages = Channel<ChannelMessage>()

  init {
    // Attempt to initialize a connection.
    runBlocking {
      conn.attemptConnection(attempts = 5)

      // Load all stages.
      for (stage in stages) {
        val payload = serialize(stage)
        grpc.load(payload)
      }
    }
  }

  /**
   * Called when a message is received from the gRPC server. The message is then routed to the
   * broker, or the appropriate channel is closed.
   *
   * @param message The message received from the gRPC server.
   * @throws UnrecognizedRequestException If the message type is not recognized.
   */
  private fun incomingMessageHandler(message: ChannelMessage) =
      when (message.type) {
        ChannelMessageType.DATA -> {
          val data = message.data.bytes.toByteArray()
          broker.send(message.channel.uri, data)
        }
        ChannelMessageType.CLOSE -> {
          broker.unregister(message.channel.uri)
        }
        else -> {
          throw UnrecognizedRequestException()
        }
      }

  /**
   * Shuts down the connection to the gRPC server. This function will block until the connection is
   * closed.
   */
  override suspend fun exit() {
    conn.shutdown()
  }

  /**
   * Propagates broker messages to the gRPC server.
   *
   * @param uri The URI of the channel.
   * @param data The data to send to the channel.
   */
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
   */
  override suspend fun exec() {
    grpc.exec(messages.receiveAsFlow()).collect(::incomingMessageHandler)
  }

  companion object {
    /**
     * Create a new process which hosts a GRPCRunner. If the process exits, the runner will also
     * exit.
     *
     * @param runner The runner to create a new instance of.
     * @param stages The stages to load in the runner.
     * @return A new GRPCRunner.
     */
    fun hostLocally(runner: IRRunner, stages: Collection<IRStage>): GRPCRunner {
      require(runner.type == RunnerType.GRPC) { "Runner must be of type GRPC." }

      // Create a new config for the runner. We run the server on a random port in [5000-10000] for
      // now, but should be configurable later.
      val port = Random.nextUInt(5000u, 10_000u)
      val config = GRPCConfig(runner.uri, "127.0.0.1", port.toInt())

      // Configure and build the process.
      val cmd = "${runner.entrypoint} localhost $port"
      val builder = ProcessBuilder(cmd.split(" "))
      val dir = File(runner.directory!!.rawPath())
      builder.directory(dir)

      // Start the process.
      val processManager = ProcessManager(builder)

      // Create a new runner.
      val grpcRunner = GRPCRunner(config, stages)
      processManager.onExit { grpcRunner.exit() }
      return grpcRunner
    }
  }
}
