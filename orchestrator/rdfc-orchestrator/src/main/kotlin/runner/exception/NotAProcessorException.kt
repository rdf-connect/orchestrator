package technology.idlab.runner.exception

import technology.idlab.runner.RunnerException

/**
 * Thrown if the implementation of a processor is invalid.
 *
 * @param processorUri The URI of the processor which is invalid.
 */
class NotAProcessorException(private val processorUri: String) : RunnerException()
