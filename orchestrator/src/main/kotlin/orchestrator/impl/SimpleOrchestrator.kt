package technology.idlab.orchestrator.impl

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import technology.idlab.broker.Broker
import technology.idlab.broker.impl.SimpleBroker
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.orchestrator.Orchestrator
import technology.idlab.orchestrator.Orchestrator.Status
import technology.idlab.runner.Runner
import technology.idlab.util.Log

class SimpleOrchestrator(
    /** All stages in the pipeline. */
    stages: List<IRStage>,
    /** List of all runners. */
    runners: List<IRRunner>
) : Orchestrator {
  /** Message broker. */
  private val broker: Broker<ByteArray>

  /** Runners by URI. */
  private val runners: Map<String, Runner>

  /** The current status of the runner. */
  override var status = Status.CREATED
    set(value) {
      Log.shared.debug { "Orchestrator status changed to: $value" }
      field = value
    }

  /** Load all stages into their respective runners. */
  init {
    this.status = Status.INITIALISING

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
    broker = SimpleBroker(this.runners.values)

    // Ready for execution.
    status = Status.READY
  }

  /** Execute all stages in all the runtimes. */
  override suspend fun exec() = coroutineScope {
    status = Status.RUNNING

    // Execute all runners in parallel.
    val executions = mutableListOf<Deferred<Unit>>()
    for ((_, runner) in runners) {
      executions.add(async { runner.exec() })
    }

    // Wait for all runners to finish.
    try {
      executions.awaitAll()
    } catch (e: Exception) {
      status = Status.FAILED
      return@coroutineScope
    }

    status = Status.SUCCESS
  }
}
