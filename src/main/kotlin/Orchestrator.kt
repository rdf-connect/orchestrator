package technology.idlab

import kotlin.concurrent.thread
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runBlocking
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

  /**
   * A channel which listens to all incoming messages and distributes them according to the topology
   * of the runners.
   */
  private val channel =
      Channel<Runner.Payload>().also {
        thread {
          runBlocking {
            for (payload in it) {
              // Special URI for printing to the console.
              if (payload.destinationURI == "print") {
                println(payload.data.decodeToString())
                continue
              }

              // Get the runner and send the message.
              val runner = readers[payload.destinationURI]
              runner!!.getIncomingChannel().send(payload)
            }
          }
        }
      }

  /** An exhaustive list of all runners. */
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
    runners.map { async { it.exec() } }.forEach { it.await() }
    Log.shared.info("All stages are online.")
  }

  /** Get a lazy evaluated runner. */
  private fun getRuntime(target: Runner.Target): Runner {
    return when (target) {
      Runner.Target.JVM -> this.jvmRunner
      Runner.Target.NODEJS -> this.nodeRunner
    }
  }
}
