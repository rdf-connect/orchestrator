package technology.idlab.rdfc.parser.exception

import technology.idlab.rdfc.parser.ParserException

class MissingArgumentsException(private val uri: String) : ParserException()
