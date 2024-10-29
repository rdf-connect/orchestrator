package technology.idlab.runner.exception

import technology.idlab.rdfc.core.RDFCException
import technology.idlab.rdfc.core.intermediate.runner.RunnerType

/**
 * A known type was used, but it has no corresponding implementation.
 *
 * @param type The type which was used, but not implemented.
 */
class UnsupportedRunnerTypeException(private val type: RunnerType) : RDFCException()
