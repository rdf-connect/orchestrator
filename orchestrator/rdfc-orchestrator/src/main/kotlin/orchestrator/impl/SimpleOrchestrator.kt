package technology.idlab.orchestrator.impl

import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import technology.idlab.broker.Broker
import technology.idlab.broker.simple.SimpleBroker
import technology.idlab.orchestrator.Orchestrator
import technology.idlab.rdfc.parser.Parser
import technology.idlab.runner.Runner

/**
 * A simple implementation of an orchestrator which only succeeds if all runners succeed without
 * intervention.
 *
 * @param parser The parser which is used to parse the configuration.
 */
class SimpleOrchestrator(parser: Parser) : Orchestrator {
  /** Message broker. */
  private val broker: Broker<ByteArray>

  /** All the runners used in the pipeline. */
  private val runners: List<Runner>

  /** Load all stages into their respective runners. */
  init {
    val runners = mutableListOf<Runner>()

    for (runner in parser.runners()) {
      val stages = parser.stages(runner)
      runners.add(Runner.from(runner, stages))
    }

    this.runners = runners
  }

  /** Initialize a broker. */
  init {
    this.broker = SimpleBroker(this.runners)
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
