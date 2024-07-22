package technology.idlab.runner

import kotlinx.coroutines.channels.Channel
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.impl.grpc.HostedGRPCRunner
import technology.idlab.runner.impl.jvm.JVMRunner
import technology.idlab.util.Log

abstract class Runner(
    val fromProcessors: Channel<Payload>,
) {
  /** The contents of a channel message. */
  data class Payload(
      // The URI of the reader which the message was sent to.
      val channel: String,
      // The data of the message.
      val data: ByteArray,
  )

  /* Messages which are destined to a processor inside the runner. */
  val toProcessors = Channel<Payload>()

  /** Register and prepare a stage inside the runtime. */
  abstract suspend fun load(processor: IRProcessor, stage: IRStage)

  /** Start pipeline execution. */
  abstract suspend fun exec()

  /** Attempt to exit the pipeline gracefully. */
  open suspend fun exit() {
    Log.shared.debug("Closing channels.")
    fromProcessors.close()
    toProcessors.close()
  }

  companion object {
    private fun builtIn(uri: String, channel: Channel<Payload>): Runner {
      return when (uri) {
        "https://www.rdf-connect.com/#JVMRunner" -> JVMRunner(channel)
        else -> Log.shared.fatal("Unknown built in runner: $uri")
      }
    }

    fun from(runner: IRRunner, channel: Channel<Payload>): Runner {
      Log.shared.info("Creating runner: ${runner.uri}")

      when (runner.type) {
        IRRunner.Type.GRPC -> {
          runner.entrypoint ?: Log.shared.fatal("No entrypoint provided for GRPCRunner.")
          runner.directory ?: Log.shared.fatal("No directory provided for GRPCRunner.")
          return HostedGRPCRunner.create(runner.entrypoint, runner.directory, channel)
        }
        IRRunner.Type.BUILT_IN -> {
          return builtIn(runner.uri, channel)
        }
        else -> {
          Log.shared.fatal("Unknown runner type: ${runner.type}")
        }
      }
    }
  }
}
