package technology.idlab

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.broker.Broker
import technology.idlab.broker.impl.SimpleBroker
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.Runner
import technology.idlab.util.Log

class Orchestrator(
    /** All stages in the pipeline. */
    stages: List<IRStage>,
    /** All available processors. */
    processors: List<IRProcessor>,
    /** List of all runners. */
    runners: List<IRRunner>
) {
  /** Message broker. */
  private val broker: Broker<ByteArray> = SimpleBroker()

  /** Stages by URI. */
  private val stages = stages.associateBy { it.uri }

  /** Runners by URI. */
  private val runners =
      runners.associateBy { it.uri }.mapValues { (_, runner) -> Runner.from(runner, broker) }

  /** Processors by URI. */
  private val processors = processors.associateBy { it.uri }

  /** Load all stages into their respective runners. */
  init {
    runBlocking {
      for ((_, stage) in this@Orchestrator.stages) {
        // Load stage.
        val processor = this@Orchestrator.processors[stage.processorURI]!!
        val runner = this@Orchestrator.runners[processor.target]!!
        runner.load(processor, stage)
      }
    }
  }

  /** Execute all stages in all the runtimes. */
  suspend fun exec() = coroutineScope {
    runners.values
        .map {
          launch {
            Log.shared.debug { "Executing: ${it.uri}" }
            it.exec()
            Log.shared.debug { "Execution finished: ${it.uri}" }
          }
        }
        .forEach { it.join() }
  }
}
