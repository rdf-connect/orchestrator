package technology.idlab.parser

import java.io.File
import technology.idlab.parser.impl.JenaParser
import technology.idlab.parser.intermediate.IRPackage
import technology.idlab.parser.intermediate.IRPipeline
import technology.idlab.parser.intermediate.IRProcessor

/**
 * Parse an RDF file into an intermediate representation, and validate it against the ontology and
 * SHACL shapes.
 */
abstract class Parser(file: File) {
  /** The pipelines in the current configuration. */
  abstract val pipelines: List<IRPipeline>

  /** The packages in the current configuration. */
  abstract val packages: List<IRPackage>

  /** List of all known processors. */
  abstract val processors: List<IRProcessor>

  companion object {
    fun using(file: File): Parser {
      return JenaParser(file)
    }
  }
}
