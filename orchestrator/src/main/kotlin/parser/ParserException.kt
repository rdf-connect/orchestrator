package technology.idlab.parser

import technology.idlab.RDFCException

/** An exception which was thrown during the parsing of a file. */
abstract class ParserException : RDFCException() {
  /** The argument in the SHACL shape is not a recognized data type. */
  class UnknownDataType(private val uri: String) : ParserException()

  class NoShaclType(private val uri: String) : ParserException()

  class ConflictingShaclType(private val uri: String) : ParserException()

  class NoShaclPropertyName(private val uri: String) : ParserException()

  class NoShaclPropertyFound(private val uri: String) : ParserException()

  class NoRunnerType(private val uri: String) : ParserException()

  class UnknownRunnerType(private val uri: String) : ParserException()

  class MissingProcessorArguments(private val uri: String) : ParserException()

  class InvalidMetadata(private val uri: String) : ParserException()

  class InvalidEntrypoint(private val uri: String) : ParserException()
}
