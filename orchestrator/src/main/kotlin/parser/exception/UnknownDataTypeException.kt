package technology.idlab.parser.exception

import technology.idlab.parser.ParserException

/** The argument in the SHACL shape is not a recognized data type. */
class UnknownDataTypeException(private val uri: String) : ParserException()
