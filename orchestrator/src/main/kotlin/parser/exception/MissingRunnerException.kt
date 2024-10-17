package technology.idlab.parser.exception

import technology.idlab.parser.ParserException

/**
 * The processor did not list a target runner.
 *
 * @param uri The URI of the processor.
 */
class MissingRunnerException(private val uri: String) : ParserException()
