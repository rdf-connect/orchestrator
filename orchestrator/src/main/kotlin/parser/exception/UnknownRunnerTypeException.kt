package technology.idlab.parser.exception

import technology.idlab.parser.ParserException

class UnknownRunnerTypeException(private val uri: String) : ParserException()
