package parser.impl

import java.io.File
import parser.ParserTest
import technology.idlab.parser.impl.TomlParser

class TomlParserTest : ParserTest() {
  // Location of the TOML file.
  private val url = this::class.java.getResource("/pipelines/basic.toml")

  // File object for the TOML file.
  private val file = File(url!!.toURI())

  // TOML parser.
  override val parser = TomlParser(file)
}
