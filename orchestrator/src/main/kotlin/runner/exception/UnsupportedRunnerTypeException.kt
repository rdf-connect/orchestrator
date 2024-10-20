package technology.idlab.runner.exception

import technology.idlab.RDFCException
import technology.idlab.intermediate.runner.RunnerType

/**
 * A known type was used, but it has no corresponding implementation.
 *
 * @param type The type which was used, but not implemented.
 */
class UnsupportedRunnerTypeException(private val type: RunnerType) : RDFCException()
