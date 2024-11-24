package technology.idlab.rdfc.orchestrator.runner.exception

import technology.idlab.rdfc.core.RDFCException
import technology.idlab.rdfc.intermediate.runner.RunnerType

/**
 * A known type was used, but it has no corresponding implementation.
 *
 * @param type The type which was used, but not implemented.
 */
class UnsupportedRunnerTypeException(private val type: RunnerType) : RDFCException() {
  override val message = "The runner type could not be found: ${type.name}"
}
