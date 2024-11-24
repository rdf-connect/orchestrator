package technology.idlab.rdfc.parser.exception

import technology.idlab.rdfc.parser.ParserException

class InvalidEntrypointException(private val uri: String) : ParserException() {
  override val message = "The entry point is invalid: $uri"
}
