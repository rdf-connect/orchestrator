package technology.idlab.parser

import technology.idlab.intermediate.IRDependency
import technology.idlab.intermediate.IRPackage
import technology.idlab.intermediate.IRPipeline
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage

/**
 * Parse an RDF file into an intermediate representation, and validate it against the ontology and
 * SHACL shapes.
 */
interface Parser {
  /** The pipelines in the current configuration. */
  fun pipelines(): List<IRPipeline>

  /** The packages in the current configuration. */
  fun packages(): List<IRPackage>

  /** List of all known processors. */
  fun processors(): List<IRProcessor>

  /** List of all known runners. */
  fun runners(): List<IRRunner>

  /** List of all known dependencies. */
  fun dependencies(): List<IRDependency>

  /**
   * Get a runner by its URI.
   *
   * @param id The URI of the runner.
   * @return The runner with the given URI.
   */
  fun runner(uri: String): IRRunner

  /**
   * Get all stages for a given runner.
   *
   * @param runner The runner to get stages for.
   * @return The stages for the given runner.
   */
  fun stages(runner: IRRunner): List<IRStage>
}
