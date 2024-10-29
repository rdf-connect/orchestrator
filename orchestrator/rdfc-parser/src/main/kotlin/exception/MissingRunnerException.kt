package technology.idlab.rdfc.parser.exception

import technology.idlab.rdfc.parser.ParserException

/**
 * The processor did not list a target runner.
 *
 * @param uri The URI of the processor.
 */
class MissingRunnerException(private val uri: String) : ParserException()
