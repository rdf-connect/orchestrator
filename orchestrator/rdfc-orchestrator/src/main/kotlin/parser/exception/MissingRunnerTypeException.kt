package technology.idlab.parser.exception

import technology.idlab.parser.ParserException

class MissingRunnerTypeException(private val uri: String) : ParserException()
