package technology.idlab.parser

import java.io.File
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import technology.idlab.extensions.validate
import technology.idlab.parser.impl.parseDependencies
import technology.idlab.parser.impl.parsePackages
import technology.idlab.parser.impl.parsePipelines
import technology.idlab.parser.intermediate.IRDependency
import technology.idlab.parser.intermediate.IRPackage
import technology.idlab.parser.intermediate.IRPipeline
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.resolver.Resolver

/**
 * Parse an RDF file into an intermediate representation, and validate it against the ontology and
 * SHACL shapes.
 */
class Parser(file: File) {
  /** The Apache Jena model. */
  private val model: Model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)

  /** The pipelines in the current configuration. */
  val pipelines: List<IRPipeline>

  /** The packages in the current configuration. */
  val packages: List<IRPackage>

  /** List of all known processors. */
  val processors: List<IRProcessor>

  init {
    // Load the RDF-Connect ontology.
    val resource = this::class.java.getResource("/pipeline.ttl")
    val config = resource!!.path!!
    this.load(config)

    // Load the pipeline file into the parser.
    this.load(file.path)

    // Retrieve dependencies.
    val dependencies = this.dependencies()

    // Resolve all dependencies.
    dependencies.forEach {
      val path = Resolver.resolve(it)
      this.load(path.toString())
    }

    // Since we updated the model, we will once again check if the SHACL shapes are valid.
    this.model.validate()

    // Parse the file.
    this.pipelines = this.pipelines()
    this.packages = this.packages()
    this.processors = this.packages.map { it.processors }.flatten()
  }

  /** Parse the file as a list of pipelines, returning its containing stages and dependencies. */
  private fun pipelines(): List<IRPipeline> {
    return model.parsePipelines()
  }

  /** Parse the model as a list of packages, returning the provided processors inside. */
  private fun packages(): List<IRPackage> {
    return model.parsePackages()
  }

  /** Retrieve all dependencies in a given file. */
  private fun dependencies(): List<IRDependency> {
    return model.parseDependencies(null as Resource?)
  }

  /** Load an additional file into the parser. */
  private fun load(path: String) {
    this.model.read(path, "TURTLE")
  }
}
