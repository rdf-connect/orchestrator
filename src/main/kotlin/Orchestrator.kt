package technology.idlab

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import runner.Runner
import runner.impl.NodeRunner
import runner.jvm.JVMRunner
import technology.idlab.intermediate.IRPipeline
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.util.Log

class Orchestrator(private val pipeline: IRPipeline, processors: List<IRProcessor>) {
  /** An exhaustive list of all runners. */
  private val channel = Channel<Runner.Payload>()
  private val jvmRunner = JVMRunner(channel)
  private val nodeRunner = NodeRunner(channel, 5000)
  private val runners = listOf(nodeRunner, jvmRunner)

  private val processors = processors.associateBy { it.uri }

  /** A map of all channel URIs and their readers. */
  private val readers = mutableMapOf<String, Runner>()

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
    Log.shared.info("Bringing all stages online.")
    val runnerJobs = runners.map { launch { it.exec() } }

    // Route messages.
    while (isActive) {
      withTimeout(1000) {
        val message = channel.receive()
        val target = readers[message.channel]!!
        Log.shared.info(
            "Brokering message '${message.data.decodeToString()}' to ${message.channel}.")
        target.toProcessors.send(message)
      }
    }
    Log.shared.debug("End routing messages between runners.")

    // Await all runners.
    runnerJobs.forEach { it.join() }
  }

  /** Get a lazy evaluated runner. */
  private fun getRuntime(target: Runner.Target): Runner {
    return when (target) {
      Runner.Target.JVM -> this.jvmRunner
      Runner.Target.NODEJS -> this.nodeRunner
    }
  }
}
