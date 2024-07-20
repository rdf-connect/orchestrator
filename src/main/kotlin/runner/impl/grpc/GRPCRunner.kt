package technology.idlab.runner.impl.grpc

import RunnerGrpcKt
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.StatusException
import kotlin.concurrent.thread
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import runner.Runner
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.util.Log

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
    val payload = stage.toGRPC(processor.toGRPC())

    try {
      grpc.load(payload)
    } catch (e: StatusException) {
      Log.shared.fatal("Failed to load stage: ${e.message}")
    }
  }

  override suspend fun exec() = coroutineScope {
    // Create a flow for outgoing messages.
    val toGRPCProcessors =
        toProcessors.receiveAsFlow().map {
          Log.shared.debug { "'${it.channel}' -> [${it.data.size} bytes]" }

          val builder = Index.ChannelData.newBuilder()
          builder.setDestinationUri(it.channel)
          builder.setData(ByteString.copyFrom(it.data))
          builder.build()
        }

    // Route messages from and into the gRPC server.
    val router = launch {
      Log.shared.debug("Begin routing messages in GRPCRunner.")

      // Create a flow for incoming messages.
      grpc
          .channel(toGRPCProcessors)
          .map { Payload(it.destinationUri, it.data.toByteArray()) }
          .collect {
            Log.shared.debug { "'${it.channel}' <- [${it.data.size} bytes]" }

            try {
              fromProcessors.send(it)
            } catch (e: CancellationException) {
              Log.shared.debug("Cancellation exception: ${e.message}")
            }
          }

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
