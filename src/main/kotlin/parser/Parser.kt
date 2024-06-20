package technology.idlab.parser

import java.io.File
import technology.idlab.parser.impl.TomlParser
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

/*
 * The Parser class provides a generic way to construct all the components of a pipeline. In theory,
 * this would allow us to extend the configuration possibilities with new formats, such as JSON or
 * YAML.
 */
abstract class Parser {
  /* Retrieve all the declared processors in the file. */
  abstract fun processors(): List<IRProcessor>

  /* Retrieve all the declared stages in the file. */
  abstract fun stages(): List<IRStage>

  companion object {
    /* Create a parser based on the file extension. */
    fun create(file: File): Parser {
      return when (file.extension) {
        "toml" -> TomlParser(file)
        else -> Log.shared.fatal("Unsupported file extension: ${file.extension}")
      }
    }
  }
}
