package runner

import kotlinx.coroutines.channels.Channel
import runner.impl.HostedGRPCRunner
import runner.jvm.JVMRunner
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
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
    fun from(runner: IRRunner, channel: Channel<Payload>): Runner {
      Log.shared.info("Creating runner: ${runner.uri}")

      if (runner.type == IRRunner.Type.GRPC) {
        runner.entrypoint ?: Log.shared.fatal("No entrypoint provided for GRPCRunner.")
        runner.directory ?: Log.shared.fatal("No directory provided for GRPCRunner.")
        return HostedGRPCRunner(runner.entrypoint, runner.directory, channel)
      } else if (runner.uri == "https://rdf-connect.com/#JVMRunner") {
        return JVMRunner(channel)
      } else {
        Log.shared.fatal("Unknown runner type: ${runner.type}")
      }
    }
  }
}
