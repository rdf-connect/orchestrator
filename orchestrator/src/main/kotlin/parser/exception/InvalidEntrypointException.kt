package technology.idlab.parser.exception

import technology.idlab.parser.ParserException

class InvalidEntrypointException(private val uri: String) : ParserException()
