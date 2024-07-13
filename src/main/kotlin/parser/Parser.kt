package technology.idlab.parser

import java.io.File
import technology.idlab.intermediate.IRPackage
import technology.idlab.intermediate.IRPipeline
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.parser.impl.JenaParser

/**
 * Parse an RDF file into an intermediate representation, and validate it against the ontology and
 * SHACL shapes.
 */
abstract class Parser {
  /** The pipelines in the current configuration. */
  abstract val pipelines: List<IRPipeline>

  /** The packages in the current configuration. */
  abstract val packages: List<IRPackage>

  /** List of all known processors. */
  abstract val processors: List<IRProcessor>

  /** List of all known runners. */
  abstract val runners: List<IRRunner>

  companion object {
    fun using(file: File): Parser {
      return JenaParser(file)
    }
  }
}
