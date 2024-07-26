package technology.idlab

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.broker.Broker
import technology.idlab.broker.impl.SimpleBroker
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.Runner
import technology.idlab.util.Log

class Orchestrator(
    /** All stages in the pipeline. */
    stages: List<IRStage>,
    /** List of all runners. */
    runners: List<IRRunner>
) {
  /** Message broker. */
  private val broker: Broker<ByteArray>

  /** Stages by URI. */
  private val stages = stages.associateBy { it.uri }

  /** Runners by URI. */
  private val runners =
      runners.associateBy { it.uri }.mapValues { (_, runner) -> Runner.from(runner) }

  /** Load all stages into their respective runners. */
  init {
    runBlocking {
      for ((_, stage) in this@Orchestrator.stages) {
        // Load stage.
        val runner = this@Orchestrator.runners[stage.processor.target]!!
        runner.load(stage)
      }
    }
  }

  init {
    this.broker = SimpleBroker(this.runners.values)
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
