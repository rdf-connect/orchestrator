package technology.idlab

import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import runner.Runner
import technology.idlab.intermediate.IRPipeline
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.util.Log

class Orchestrator(
    private val pipeline: IRPipeline,
    processors: List<IRProcessor>,
    runners: List<IRRunner>
) {
  /** An exhaustive list of all runners. */
  private val channel = Channel<Runner.Payload>()

  private val runners = runners.associateBy { it.uri }.mapValues { Runner.from(it.value, channel) }

  private val processors = processors.associateBy { it.uri }

  /** A map of all channel URIs and their readers. */
  private val readers = mutableMapOf<String, Runner>()

  init {
    Log.shared.info("Bringing runners online")
  }

  init {
    runBlocking { pipeline.stages.forEach { prepare(it) } }
  }

  /** Prepare a stage inside of it's corresponding runtime. */
  private suspend fun prepare(stage: IRStage) {
    // Get the corresponding runner.
    val processor = this.processors[stage.processorURI]!!
    val runner = getRuntime(processor.target)
    runner.load(processor, stage)

    // Find all the readers in the stage.
    stage.getReaders(processor).forEach { this.readers[it] = runner }
  }

  /** Execute all stages in all the runtimes. */
  suspend fun exec() = coroutineScope {
    // Route messages.
    val router = launch {
      Log.shared.info("Begin routing messages between runners.")
      while (isActive) {
        try {
          withTimeout(1000) {
            val message = channel.receive()
            val target = readers[message.channel]!!
            Log.shared.info(
                "Brokering message '${
                  message.data.decodeToString().replace("\n", "\\n")
                }' to ${message.channel}.")
            target.toProcessors.send(message)
          }
        } catch (_: TimeoutCancellationException) {}
      }
    }

    // Notify when the router exits.
    router.invokeOnCompletion { Log.shared.debug("End routing messages between runners.") }

    // Execute all stages.
    Log.shared.info("Bringing all stages online.")
    runners.values.map { launch { it.exec() } }.forEach { it.join() }

    router.cancel()
  }

  /** Get a lazy evaluated runner. */
  private fun getRuntime(uri: String): Runner {
    return this.runners[uri] ?: Log.shared.fatal("Unknown runner: $uri")
  }
}
