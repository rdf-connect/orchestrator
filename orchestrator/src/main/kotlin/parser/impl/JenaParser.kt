package technology.idlab.parser.impl

import java.io.File
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.Shapes
import org.apache.jena.vocabulary.RDF
import technology.idlab.RDFC
import technology.idlab.extensions.asArguments
import technology.idlab.extensions.getCollection
import technology.idlab.extensions.node
import technology.idlab.extensions.objectOfProperty
import technology.idlab.extensions.property
import technology.idlab.extensions.targeting
import technology.idlab.extensions.validate
import technology.idlab.intermediate.Argument
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRDependency
import technology.idlab.intermediate.IRPackage
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRPipeline
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.intermediate.LiteralArgument
import technology.idlab.intermediate.LiteralParameter
import technology.idlab.intermediate.NestedArgument
import technology.idlab.intermediate.NestedParameter
import technology.idlab.intermediate.Parameter
import technology.idlab.parser.Parser
import technology.idlab.parser.ParserException
import technology.idlab.util.Log

class JenaParser(
    /** A file pointer to the pipeline configuration entrypoint. */
    files: List<File>,
) : Parser {
  /** The Apache Jena model. */
  private val model: Model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)

  /** Model wrapper for accessing the known SHACL shapes. */
  private val shapes: Shapes

  init {
    Log.shared.debug { "Parsing: ${files.joinToString(", ")}" }

    // Load the RDF-Connect ontology.
    val resource = this::class.java.getResource("/pipeline.ttl")
    val config = resource!!.path!!
    this.model.read(config, "TURTLE")

    // Load the pipeline file into the parser.
    for (file in files) {
      this.model.read(file.path, "TURTLE")
    }

    // Create a new shapes object.
    this.shapes = Shapes.parse(model)

    // Confirm that the values loaded into the model are valid.
    this.model.validate()
  }

  private fun arguments(uri: Resource, parameters: IRParameter): IRArgument {
    val result = arguments(uri, parameters.type)
    return IRArgument(parameters, result)
  }

  /**
   * Parse the arguments of a stage. This is a recursive implementation that will automatically
   * parse nested classes. Recursion will continue until all objects found are literals.
   */
  private fun arguments(uri: Resource, parameters: Map<String, Parameter>): Map<String, Argument> {
    val result = mutableMapOf<String, Argument>()

    for ((name, parameter) in parameters) {
      val path = model.createProperty(parameter.uri)
      val arguments = model.listObjectsOfProperty(uri, path).toList()

      // If the value is null, we may either skip it if it's not required or throw an exception if
      // it is.
      if (arguments.isEmpty()) {
        if (!parameter.optional) {
          throw Exception()
        } else {
          continue
        }
      }

      // If the parameter is simple, just add it to the list as a literal or resource.
      if (parameter is LiteralParameter) {
        val values =
            arguments.map {
              if (it.isLiteral) {
                it.asLiteral().value.toString()
              } else {
                it.asResource().toString()
              }
            }

        val argument = result.getOrPut(name) { LiteralArgument(parameter) }

        if (argument is LiteralArgument) {
          argument.values.addAll(values)
        } else {
          throw IllegalStateException()
        }
      }

      if (parameter is NestedParameter) {
        val values = arguments.map { arguments(it.asResource(), parameter.type) }

        val argument = result.getOrPut(name) { NestedArgument(parameter) }

        if (argument is NestedArgument) {
          argument.values.addAll(values)
        } else {
          throw IllegalStateException()
        }
      }
    }

    return result
  }

  /**
   * Parse all stages of a given pipeline.
   *
   * @param pipeline The pipeline to parse.
   * @return The list of stages.
   * @see stage
   */
  private fun stages(pipeline: Resource): List<IRStage> {
    return model.listObjectsOfProperty(pipeline, RDFC.stages).toList().map {
      this.stage(it.asResource())
    }
  }

  /**
   * Parse a single stage by URI.
   *
   * @param stage The URI of the stage.
   * @return The parsed stage.
   */
  private fun stage(stage: Resource): IRStage {
    val processorURI = model.objectOfProperty(stage, RDF.type)!!.asResource()
    val processor = processor(processorURI)
    val argumentsURI = model.objectOfProperty(stage, RDFC.arguments)!!.asResource()
    val arguments = arguments(argumentsURI, processor.parameters)
    return IRStage(stage.toString(), processor, arguments)
  }

  override fun pipelines(): List<IRPipeline> {
    val pipelines = model.listSubjectsWithProperty(RDF.type, RDFC.pipeline).toList()
    return pipelines.map { pipeline(it) }
  }

  /**
   * Parse a pipeline by URI from the model.
   *
   * @param pipeline The URI of the pipeline.
   * @return The parsed pipeline.
   */
  private fun pipeline(pipeline: Resource): IRPipeline {
    val dependencyResources = model.listObjectsOfProperty(pipeline, RDFC.dependency).toList()
    val dependencies = dependencyResources.map { dependency(it.asResource()) }

    return IRPipeline(
        uri = pipeline.uri,
        stages = stages(pipeline),
        dependencies = dependencies,
    )
  }

  /**
   * Parse a dependency from the model by URI.
   *
   * @param dependency The resource of the dependency.
   * @return The parsed dependency.
   */
  private fun dependency(dependency: Resource): IRDependency {
    return IRDependency(dependency.uri.toString())
  }

  /**
   * Parse a processor from the model.
   *
   * @param processor URI of the processor.
   * @return The parsed processor.
   */
  private fun processor(processor: Resource): IRProcessor {
    // Determine the target runner.
    val target =
        model.objectOfProperty(processor, RDFC.target)
            ?: throw ParserException.NoRunnerSpecified(processor.uri)
    val shape = shapes.targeting(processor).single()

    // The arguments are a nested shape in the processor config.
    val parameters =
        shape.property(RDFC.arguments)?.node()?.asArguments()
            ?: throw ParserException.MissingProcessorArguments(processor.uri)

    // Parse metadata.
    val metadata = mutableMapOf<String, String>()
    for (entry in model.listObjectsOfProperty(processor, RDFC.metadata)) {
      val literal = entry.asLiteral().string
      val (key, value) = literal.split(':')
      metadata[key.trim()] = value.trim()
    }

    // Get entrypoint.
    val entrypoint = model.objectOfProperty(processor, RDFC.entrypoint)!!.toString()

    return IRProcessor(
        processor.toString(), target.toString(), entrypoint, IRParameter("", parameters), metadata)
  }

  override fun packages(): List<IRPackage> {
    val packages = model.listSubjectsWithProperty(RDF.type, RDFC.`package`).toList()
    return packages.map { `package`(it) }
  }

  override fun processors(): List<IRProcessor> {
    return packages().map { it.processors }.flatten()
  }

  override fun runners(): List<IRRunner> {
    val result = mutableListOf<IRRunner>()

    // Add all package runners.
    for (pkg in packages()) {
      result.addAll(pkg.runners)
    }

    // Retrieve built in runners.
    val builtIn = model.listSubjectsWithProperty(RDF.type, RDFC.builtInRunner).toList()
    for (uri in builtIn) {
      val runner = IRRunner(uri.toString(), type = IRRunner.Type.BUILT_IN)
      result.add(runner)
    }

    return result
  }

  override fun dependencies(): List<IRDependency> {
    val uris = model.listObjectsOfProperty(RDFC.dependency).toList()
    return uris.map { IRDependency(it.toString()) }
  }

  override fun dependencies(pipelineUri: String): List<IRDependency> {
    val pipeline = model.getResource(pipelineUri)
    val uris = model.listObjectsOfProperty(pipeline, RDFC.dependency).toList()
    return uris.map { IRDependency(it.toString()) }
  }

  private fun `package`(pkg: Resource): IRPackage {
    // Get all of its properties.
    val version = model.objectOfProperty(pkg, RDFC.version)
    val author = model.objectOfProperty(pkg, RDFC.author)
    val description = model.objectOfProperty(pkg, RDFC.description)
    val repo = model.objectOfProperty(pkg, RDFC.repo)
    val license = model.objectOfProperty(pkg, RDFC.license)
    val processorsUris = model.listObjectsOfProperty(pkg, RDFC.processors).toList()
    val processors = processorsUris.map { processor(it.asResource()) }
    val runnersUris = model.listObjectsOfProperty(pkg, RDFC.runners).toList()
    val runners = runnersUris.map { runner(it.asResource()) }

    // The preparation statements are encoded as an ordered list, and need to be handled as such.
    val prepare =
        model.objectOfProperty(pkg, RDFC.prepare)?.let { model.getCollection(it.asResource()) }

    // Parse the properties to strings if required, and return the package IR.
    return IRPackage(
        version = version?.toString(),
        author = author?.toString(),
        description = description.toString(),
        repo = repo.toString(),
        license = license.toString(),
        prepare = prepare?.map { it.toString() } ?: emptyList(),
        processors = processors,
        runners = runners,
    )
  }

  override fun runner(uri: String): IRRunner {
    val resource = model.getResource(uri)
    return runner(resource)
  }

  private fun runner(runner: Resource): IRRunner {
    val entrypoint = model.objectOfProperty(runner, RDFC.entrypoint)

    // Extract the working directory as a file.
    val wd =
        model.objectOfProperty(runner, RDFC.workingDirectory).let {
          if (it != null) {
            File(it.toString())
          } else {
            null
          }
        }

    val type =
        when (model.objectOfProperty(runner, RDF.type)) {
          RDFC.builtInRunner -> IRRunner.Type.BUILT_IN
          RDFC.grpcRunner -> IRRunner.Type.GRPC
          null -> throw ParserException.NoRunnerType(runner.uri)
          else -> throw ParserException.UnknownRunnerType(runner.uri)
        }

    // Check if the entrypoint is valid.
    if (type == IRRunner.Type.BUILT_IN && entrypoint != null) {
      throw ParserException.InvalidEntrypoint(runner.uri)
    }

    return IRRunner(runner.toString(), wd, entrypoint.toString(), type)
  }

  override fun stages(runner: IRRunner): List<IRStage> {
    return pipelines().flatMap { it.stages }.filter { it.processor.target == runner.uri }
  }
}
