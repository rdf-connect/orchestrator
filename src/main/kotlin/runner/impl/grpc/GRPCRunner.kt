package technology.idlab.runner.impl.grpc

import RunnerGrpcKt
import channel
import channelData
import channelMessage
import com.google.protobuf.ByteString
import dataOrNull
import io.grpc.ConnectivityState
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import kotlin.concurrent.thread
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.Runner
import technology.idlab.util.Log
import technology.idlab.util.retries

/**
 * This runner has GRPC built-in, so the only configuration that an extending class needs to provide
 * is the host and port of the GRPC server, as well as actually booting the process.
 */
abstract class GRPCRunner(
    /** The channel to receive messages from the processors. */
    fromProcessors: Channel<Payload>,
    /** Location of the gRPC server. */
    config: Config,
) : Runner(fromProcessors) {
  /**
   * Define how the generic GRPCRunner should communicate over the network.
   *
   * @param host The host of the gRPC server.
   * @param port The port of the gRPC server.
   */
  data class Config(val host: String, val port: Int) {
    fun connect(): ManagedChannel {
      return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    }
  }

  // Connect to the server at initialisation.
  private val conn = config.connect()

  // Create a gRPC stub.
  private val grpc = RunnerGrpcKt.RunnerCoroutineStub(conn)

  init {
    Log.shared.debug { "Waiting for connection." }

    runBlocking {
      retries(5) {
        if (conn.getState(true) != ConnectivityState.READY) {
          throw Exception("gRPC connection not ready.")
        }
      }
    }
  }

  /**
   * This flow collector is used to route messages from the gRPC server to the main orchestrator. It
   * maps the incoming `ChannelMessage` to a payload, and sends it to the broker. Note that this
   * flow collector is not a coroutine, but a lambda that is called by the gRPC server.
   */
  private val fromProcessorsCollector =
      FlowCollector<ChannelOuterClass.ChannelMessage> {
        if (it.type == ChannelOuterClass.ChannelMessageType.CLOSE) {
          Log.shared.debug { "Channel closing: ${it.channel.uri}" }
          return@FlowCollector
        }

        if (it.type == ChannelOuterClass.ChannelMessageType.UNRECOGNIZED) {
          Log.shared.fatal("Channel '${it.channel.uri}' received an unrecognized message type.")
        }

        // We can now assume that the message is of type DATA.
        assert(it.type == ChannelOuterClass.ChannelMessageType.DATA)

        // If no data was received, log an error and substitute an empty byte array.
        val data =
            it.dataOrNull?.bytes?.toByteArray()
                ?: run {
                  Log.shared.severe("Channel '${it.channel.uri}' received a message with no data.")
                  return@run "".toByteArray()
                }

        // Send message to the broker.
        val payload = Payload(it.channel.uri, data)
        fromProcessors.send(payload)
      }

  // Map the incoming log messages to the shared logger.
  private val logger =
      thread(isDaemon = true) {
        runBlocking {
          grpc.log(empty).collect {
            when (it.level ?: Index.LogLevel.UNRECOGNIZED) {
              Index.LogLevel.DEBUG -> Log.shared.debug(it.message, location = it.location)
              Index.LogLevel.INFO -> Log.shared.info(it.message, location = it.location)
              Index.LogLevel.SEVERE -> Log.shared.severe(it.message, location = it.location)
              Index.LogLevel.FATAL -> Log.shared.fatal(it.message, location = it.location)
              Index.LogLevel.UNRECOGNIZED ->
                  Log.shared.fatal("Unknown log level.", location = it.location)
            }
          }
        }
      }

  override suspend fun exit() {
    Log.shared.debug("Exiting GRPCRunner.")
    super.exit()

    Log.shared.debug("Shutting down connection.")
    conn.shutdown()
  }

  override suspend fun load(processor: IRProcessor, stage: IRStage) {
    Log.shared.debug { "Loading stage '${stage.uri}'." }

    val payload = stage.toGRPC(processor.toGRPC())
    try {
      grpc.load(payload)
    } catch (e: StatusException) {
      Log.shared.fatal("Failed to load stage: ${e.cause}")
    }
  }

  override suspend fun exec() = coroutineScope {
    // Create a flow for outgoing messages.
    val toGRPCProcessors =
        toProcessors.receiveAsFlow().map {
          Log.shared.debug { "'${it.channel}' -> [${it.data.size} bytes]" }

          channelMessage {
            channel = channel { uri = it.channel }
            data = channelData { bytes = ByteString.copyFrom(it.data) }
          }
        }

    // Route messages from and into the gRPC server.
    val router = launch {
      Log.shared.debug("Begin routing messages in GRPCRunner.")
      grpc.channel(toGRPCProcessors).collect(this@GRPCRunner.fromProcessorsCollector)
      Log.shared.debug("Ending routing messages in GRPCRunner.")
    }

    // Attempt to execute the pipelines.
    try {
      grpc.exec(empty)
    } catch (e: StatusException) {
      Log.shared.fatal("Failed to execute pipeline: ${e.message}")
    }

    // Wait for the router to finish.
    router.join()
  }
}
