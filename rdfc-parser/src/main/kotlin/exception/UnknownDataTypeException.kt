package technology.idlab.rdfc.parser.exception

import technology.idlab.rdfc.parser.ParserException

/** The argument in the SHACL shape is not a recognized data type. */
class UnknownDataTypeException(private val uri: String) : ParserException() {
  override val message = "The data type is unknown: $uri"
}
