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
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

class Orchestrator(stages: Set<IRStage>) {
  /** An exhaustive list of all runners. */
  private val channel = Channel<Runner.Payload>()
  private val jvmRunner = JVMRunner(channel)
  private val nodeRunner = NodeRunner(channel, 5000)
  private val runners = listOf(nodeRunner, jvmRunner)

  /** A map of all channel URIs and their readers. */
  private val readers = mutableMapOf<String, Runner>()

  init {
    /** Initialize the processors and stages in the runtimes. */
    runBlocking { stages.forEach { stage -> prepare(stage) } }
  }

  /** Prepare a stage inside of it's corresponding runtime. */
  private suspend fun prepare(stage: IRStage) {
    // Get the corresponding runner.
    val runner = getRuntime(stage.processor.target)
    runner.load(stage)

    // Find all the readers in the stage.
    stage.getReaders().forEach { this.readers[it] = runner }
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
