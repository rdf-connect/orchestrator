package technology.idlab.rdfc.parser.exception

import technology.idlab.rdfc.parser.ParserException

class UnknownRunnerTypeException(private val uri: String) : ParserException()
