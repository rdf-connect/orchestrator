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
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

class Orchestrator(stages: Set<IRStage>) {
  /** List of all processors in the pipeline. */
  private val processors = stages.map { it.processor }.toSet()

  /** An exhaustive list of all runners. */
  private val channel = Channel<Runner.Payload>()
  private val jvmRunner by lazy { JVMRunner(channel) }
  private val nodeRunner by lazy { NodeRunner(channel, 5000) }
  private val runners = listOf(nodeRunner, jvmRunner)

  /** A map of all channel URIs and their readers. */
  private val readers = mutableMapOf<String, Runner>()

  init {
    /** Initialize the processors and stages in the runtimes. */
    runBlocking {
      processors.forEach { processor -> prepare(processor) }
      stages.forEach { stage -> prepare(stage) }
    }
  }

  /** Prepare a processor inside of it's corresponding runtime. */
  private suspend fun prepare(processor: IRProcessor) {
    val runner = getRuntime(processor.target)
    runner.prepare(processor)
  }

  /** Prepare a stage inside of it's corresponding runtime. */
  private suspend fun prepare(stage: IRStage) {
    // Get the corresponding runner.
    val runner = getRuntime(stage.processor.target)
    runner.prepare(stage)

    // Find all the readers in the stage.
    val readers =
        stage.processor.parameters.filter { it.type == IRParameter.Type.READER }.map { it.name }

    // Get their concrete URIs.
    val uris = stage.arguments.filter { readers.contains(it.name) }.map { it.value[0] }

    // Add them as a channel targets.
    uris.forEach { this.readers[it] = runner }
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
