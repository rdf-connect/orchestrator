package technology.idlab

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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

  /** Runners by URI. */
  private val runners: Map<String, Runner>

  /** Load all stages into their respective runners. */
  init {
    // Associate the URI of the runner to the runner itself, as well as the set of stages.
    val result: Map<String, Pair<IRRunner, MutableSet<IRStage>>> =
        runners.associateBy { it.uri }.mapValues { (_, runner) -> Pair(runner, mutableSetOf()) }

    // Add every stage to it's corresponding runner.
    for (stage in stages) {
      result[stage.processor.target]!!.second.add(stage)
    }

    // Instantiate the runners.
    this.runners =
        result.mapValues {
          val (runner, runnerStages) = it.value
          Runner.from(runner, runnerStages)
        }

    // Register broker.
    this.broker = SimpleBroker(this.runners.values)
  }

  /** Execute all stages in all the runtimes. */
  suspend fun exec() = coroutineScope {
    // Prepare all runners for execution.
    var jobs =
        runners.values.map {
          launch {
            Log.shared.debug { "Preparing runner: ${it.uri}" }
            it.prepare()
            Log.shared.debug { "Preparation runner finished: ${it.uri}" }
          }
        }
    jobs.forEach { it.join() }

    // Start execution.
    jobs =
        runners.values.map {
          launch {
            Log.shared.debug { "Executing runner: ${it.uri}" }
            it.exec()
            Log.shared.debug { "Execution runner finished: ${it.uri}" }
          }
        }
    jobs.forEach { it.join() }
  }
}
