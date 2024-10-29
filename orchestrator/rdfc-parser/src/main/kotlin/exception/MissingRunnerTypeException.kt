package technology.idlab.rdfc.parser.exception

import technology.idlab.rdfc.parser.ParserException

class MissingRunnerTypeException(private val uri: String) : ParserException()
