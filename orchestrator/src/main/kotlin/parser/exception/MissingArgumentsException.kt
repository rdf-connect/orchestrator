package technology.idlab.parser.exception

import technology.idlab.parser.ParserException

class MissingArgumentsException(private val uri: String) : ParserException()
