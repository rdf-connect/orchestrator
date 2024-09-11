package technology.idlab.parser.impl.jena

import java.io.File
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.vocabulary.SHACLM
import org.apache.jena.vocabulary.RDF
import technology.idlab.extensions.getCollection
import technology.idlab.extensions.objectOfProperty
import technology.idlab.extensions.subjectWithProperty
import technology.idlab.extensions.validate
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRDependency
import technology.idlab.intermediate.IRPackage
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRPipeline
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.parser.Parser
import technology.idlab.resolver.Resolver
import technology.idlab.util.Log

/**
 * Parse the arguments of a stage. This is a recursive implementation that will automatically parse
 * nested classes. Recursion will continue until all objects found are literals.
 */
private fun Model.parseArguments(
    node: Resource,
    parameters: Map<String, IRParameter>
): Map<String, IRArgument> {
  val simple = mutableMapOf<String, MutableList<String>>()
  val complex = mutableMapOf<String, MutableList<Map<String, IRArgument>>>()

  // Go over each triple of the resource. If it is a literal, add it to the simple list. Otherwise,
  // call recursively and add it to the complex list.
  for (triple in listStatements(node, null, null as RDFNode?)) {
    // The predicate must equal the SHACL path.
    val path = triple.predicate

    // Skip the type predicate, if it is given.
    if (triple.predicate == RDF.type) {
      continue
    }

    // Get the name of the argument.
    val key = nameOfSHACLPath(path)
    val params = parameters[key]!!
    val value = triple.`object`

    if (isSimpleSHACLShape(path)) {
      val list = simple.getOrPut(key) { mutableListOf() }
      val v =
          if (value.isLiteral) {
            value.asLiteral().value
          } else {
            value.asResource()
          }
      list.add(v.toString())
    } else {
      val list = complex.getOrPut(key) { mutableListOf() }
      val nested = parseArguments(value.asResource(), params.getComplex())
      list.add(nested)
    }
  }

  // Combine both simple and complex mappings as a single map to IRArguments.
  return simple.mapValues { (key, value) ->
    IRArgument(simple = value, parameter = parameters[key]!!)
  } +
      complex.mapValues { (key, value) ->
        IRArgument(complex = value, parameter = parameters[key]!!)
      }
}

private fun Model.parseRunner(directory: File, runner: Resource): IRRunner {
  Log.shared.debug("Parsing runner: $runner")

  val entrypoint = objectOfProperty(runner, RDFC.entrypoint)?.toString()
  val type =
      objectOfProperty(runner, RDF.type) ?: Log.shared.fatal("No type found for runner: $runner")

  when (type) {
    RDFC.grpcRunner -> {
      return IRRunner(runner.toString(), directory, entrypoint, IRRunner.Type.GRPC)
    }
    RDFC.builtInRunner -> {
      if (entrypoint != null) {
        Log.shared.fatal("Built in runner $runner has entrypoint, but will be discarded.")
      }

      return IRRunner(runner.toString(), directory, null, IRRunner.Type.BUILT_IN)
    }
    else -> {
      Log.shared.fatal("Unknown runner type: $type")
    }
  }
}

private fun Model.parseProcessor(processor: Resource): IRProcessor {
  Log.shared.debug("Parsing processor: $processor")

  val uri = processor.toString()

  // Determine the target runner.
  val target = objectOfProperty(processor, RDFC.target)!!.toString()

  // Parse the parameters by SHACL shape.
  val shape =
      subjectWithProperty(SHACLM.targetClass, processor)
          ?: Log.shared.fatal("No shape found for processor: $processor")
  val parameters =
      listObjectsOfProperty(shape, SHACLM.property)
          .toList()
          .find {
            val path = objectOfProperty(it.asResource(), SHACLM.path)?.asResource()
            return@find path == RDFC.arguments
          }
          ?.let { objectOfProperty(it.asResource(), SHACLM.node)?.asResource() }
          ?.let { parseSHACLShape(it) }
          ?: Log.shared.fatal("No argument shape found for processor: $processor")

  // Parse metadata.
  val metadata = mutableMapOf<String, String>()
  for (entry in this.listObjectsOfProperty(processor, RDFC.metadata)) {
    val literal =
        try {
          entry.asLiteral().string
        } catch (e: Exception) {
          Log.shared.fatal("Metadata must be a literal.")
        }
    val (key, value) = literal.split(':')
    metadata[key.trim()] = value.trim()
  }

  // Get entrypoint.
  val entrypoint = objectOfProperty(processor, RDFC.entrypoint)!!.toString()

  return IRProcessor(uri, target, entrypoint, parameters, metadata)
}

private fun Model.parseStages(pipeline: Resource): List<IRStage> {
  return listObjectsOfProperty(pipeline, RDFC.stages).toList().map { stage ->
    val processorURI = objectOfProperty(stage.asResource(), RDF.type)!!.asResource()
    val processor = parseProcessor(processorURI)
    val arguments = objectOfProperty(stage.asResource(), RDFC.arguments)!!.asResource()
    IRStage(stage.toString(), processor, parseArguments(arguments, processor.parameters))
  }
}

private fun Model.parseDependencies(pipeline: Resource?): List<IRDependency> {
  return listObjectsOfProperty(pipeline, RDFC.dependency).toList().map { dependency ->
    IRDependency(uri = dependency.toString())
  }
}

private fun Model.parsePackage(directory: File, pkg: Resource): IRPackage {
  Log.shared.debug("Parsing package: $pkg")

  // Get all of its properties.
  val version = objectOfProperty(pkg, RDFC.version)
  val author = objectOfProperty(pkg, RDFC.author)
  val description = objectOfProperty(pkg, RDFC.description)
  val repo = objectOfProperty(pkg, RDFC.repo)
  val license = objectOfProperty(pkg, RDFC.license)
  val processors =
      listObjectsOfProperty(pkg, RDFC.processors).toList().map { parseProcessor(it.asResource()) }
  val runners =
      listObjectsOfProperty(pkg, RDFC.runners).toList().map {
        parseRunner(directory, it.asResource())
      }

  val prepareCollection = objectOfProperty(pkg, RDFC.prepare)
  val prepare =
      if (prepareCollection != null) {
        getCollection(prepareCollection.asResource()).map { it.toString() }
      } else {
        emptyList()
      }

  // Parse the properties to strings if required, and return the package IR.
  return IRPackage(
      directory = directory,
      version = version?.toString(),
      author = author?.toString(),
      description = description.toString(),
      repo = repo.toString(),
      license = license.toString(),
      prepare = prepare,
      processors = processors,
      runners = runners,
  )
}

private fun Model.parsePackages(directory: File): List<IRPackage> {
  return listSubjectsWithProperty(RDF.type, RDFC.`package`).toList().map {
    parsePackage(directory, it)
  }
}

private fun Model.parsePipeline(pipeline: Resource): IRPipeline {
  Log.shared.debug("Parsing pipeline: $pipeline")

  return IRPipeline(
      uri = pipeline.uri,
      stages = parseStages(pipeline),
      dependencies = parseDependencies(pipeline),
  )
}

private fun Model.parsePipelines(): List<IRPipeline> {
  return listSubjectsWithProperty(RDF.type, RDFC.pipeline).toList().map { parsePipeline(it) }
}

class JenaParser(
    /** A file pointer to the pipeline configuration entrypoint. */
    file: File,
    /** Dependency resolver. */
    private val resolver: Resolver
) : Parser {
  /** The Apache Jena model. */
  private val model: Model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)

  /** The pipelines in the current configuration. */
  override val pipelines: List<IRPipeline>

  /** The packages in the current configuration. */
  override val packages: List<IRPackage>

  /** List of all known processors. */
  override val processors: List<IRProcessor>

  /** List of all known runners. */
  override val runners: List<IRRunner>

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
    this.packages =
        dependencies
            .map {
              val path = resolver.resolve(it)
              val mdl = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
              mdl.read(path.toString(), "TURTLE")
              val result = mdl.parsePackages(path.parentFile)
              model.add(mdl)
              result
            }
            .flatten()

    // Since we updated the model, we will once again check if the SHACL shapes are valid.
    this.model.validate()

    // Parse the file.
    this.pipelines = this.pipelines()
    this.processors = this.packages.map { it.processors }.flatten()

    // Packaged runners.
    val runners = this.packages.map { it.runners }.toMutableList()

    // Retrieve built in runners.
    val builtInRunners =
        this.model.listSubjectsWithProperty(RDF.type, RDFC.builtInRunner).toList().map {
          IRRunner(it.toString(), type = IRRunner.Type.BUILT_IN)
        }
    runners.add(builtInRunners)

    // Combine both runner types.
    this.runners = runners.flatten()
  }

  /** Parse the file as a list of pipelines, returning its containing stages and dependencies. */
  private fun pipelines(): List<IRPipeline> {
    return model.parsePipelines()
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
