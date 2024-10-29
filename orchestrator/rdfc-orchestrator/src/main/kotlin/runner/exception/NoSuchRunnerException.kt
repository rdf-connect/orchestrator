package technology.idlab.rdfc.orchestrator.runner.exception

import technology.idlab.rdfc.orchestrator.runner.RunnerException

/**
 * A runner was referenced by a stage, but no such runner was found.
 *
 * @param runnerUri The URI of the runner which was not found.
 */
class NoSuchRunnerException(private val runnerUri: String) : RunnerException()
