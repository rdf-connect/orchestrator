package technology.idlab.parser

import technology.idlab.rdfc.core.intermediate.IRDependency
import technology.idlab.rdfc.core.intermediate.IRPackage
import technology.idlab.rdfc.core.intermediate.IRPipeline
import technology.idlab.rdfc.core.intermediate.IRProcessor
import technology.idlab.rdfc.core.intermediate.IRRunner
import technology.idlab.rdfc.core.intermediate.IRStage

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
   * List of all dependencies of a given pipeline.
   *
   * @param pipelineUri The URI of the pipeline as a raw string.
   */
  fun dependencies(pipelineUri: String): List<IRDependency>

  /**
   * Get a runner by its URI.
   *
   * @param uri The URI of the runner.
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
