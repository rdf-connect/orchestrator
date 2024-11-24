package technology.idlab.rdfc.orchestrator.orchestrator.impl

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import technology.idlab.rdfc.core.log.Log
import technology.idlab.rdfc.intermediate.IRRunner
import technology.idlab.rdfc.intermediate.IRStage
import technology.idlab.rdfc.orchestrator.broker.Broker
import technology.idlab.rdfc.orchestrator.broker.simple.SimpleBroker
import technology.idlab.rdfc.orchestrator.orchestrator.Orchestrator
import technology.idlab.rdfc.orchestrator.runner.Runner

/**
 * A simple implementation of an orchestrator which only succeeds if all runners succeed without
 * intervention.
 *
 * @param runners All the runners which should be instantiated before loading the stages.
 * @param stages A collection of stages which should be run. Note that all the corresponding runners
 *   should be included in the `runner` declaration.
 */
class SimpleOrchestrator(runners: Collection<IRRunner>, stages: Collection<IRStage>) :
    Orchestrator {
  /** Message broker. */
  private val broker: Broker<ByteArray>

  /** All the runners used in the pipeline. */
  private val runners: List<Runner>

  init {
    // Initialize all runners with their corresponding stages.
    val instances = mutableListOf<Runner>()
    val remainingStages = stages.toMutableList()

    for (runner in runners) {
      val targets = remainingStages.filter { it.processor.target == runner.uri }
      remainingStages.removeAll(targets)

      // Check if the runner actually has a target.
      if (targets.isEmpty()) {
        Log.shared.debug { "Runner has no targets, will not be instantiated: $runner" }
        continue
      }

      // Create the runner.
      val instance = Runner.from(runner, targets)
      instances.add(instance)
    }

    // Assert that all stages have been instantiated.
    check(remainingStages.isEmpty()) {
      val uris = remainingStages.joinToString(", ")
      "Could not find a runner for the following processors: $uris"
    }

    // Initialize a broker.
    this.broker = SimpleBroker(instances)
    this.runners = instances
  }

  /** Execute all stages in all the runtimes. */
  override suspend fun exec() = coroutineScope {
    // Execute all runners in parallel.
    val executions = mutableListOf<Deferred<Unit>>()
    for (runner in runners) {
      executions.add(async { runner.exec() })
    }

    // Wait for all runners to finish.
    executions.awaitAll()
    return@coroutineScope
  }
}
