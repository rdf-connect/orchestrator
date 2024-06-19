package parser.impl

import java.io.File
import parser.ParserTest
import technology.idlab.parser.impl.RDFParser

class RDFParserTest : ParserTest() {
  // Location of the Turtle file.
  private val url = this::class.java.getResource("/pipelines/basic.ttl")

  // File object for the Turtle file.
  private val file = File(url!!.toURI())

  // Turtle parser.
  override val parser = RDFParser(file)
}
