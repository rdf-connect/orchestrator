package technology.idlab.parser.impl.jena

import java.io.File
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.vocabulary.SHACLM
import org.apache.jena.vocabulary.RDF
import technology.idlab.InvalidWorkingDirectoryException
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
import technology.idlab.parser.ParserException
import technology.idlab.util.Log

/**
 * Parse the arguments of a stage. This is a recursive implementation that will automatically parse
 * nested classes. Recursion will continue until all objects found are literals.
 */
private fun Model.arguments(
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
      val nested = arguments(value.asResource(), params.getComplex())
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

private fun Model.runner(runner: Resource): IRRunner {
  Log.shared.debug("Parsing runner: $runner")

  val workingDirectoryPath =
      objectOfProperty(runner, RDFC.workingDirectory)?.toString()?.removePrefix("file://")
  val workingDirectory = if (workingDirectoryPath != null) File(workingDirectoryPath) else null
  if (workingDirectory != null && !workingDirectory.exists()) {
    throw InvalidWorkingDirectoryException(workingDirectory.toString())
  }

  val entrypoint = objectOfProperty(runner, RDFC.entrypoint)?.toString()
  val type = objectOfProperty(runner, RDF.type) ?: throw ParserException.NoRunnerType(runner.uri)

  when (type) {
    RDFC.grpcRunner -> {
      return IRRunner(runner.toString(), workingDirectory, entrypoint, IRRunner.Type.GRPC)
    }
    RDFC.builtInRunner -> {
      if (entrypoint != null) {
        throw ParserException.InvalidEntrypoint(runner.uri)
      }

      return IRRunner(runner.toString(), workingDirectory, null, IRRunner.Type.BUILT_IN)
    }
    else -> {
      throw ParserException.UnknownRunnerType(type.toString())
    }
  }
}

private fun Model.processor(processor: Resource): IRProcessor {
  Log.shared.debug("Parsing processor: $processor")

  val uri = processor.toString()

  // Determine the target runner.
  val target = objectOfProperty(processor, RDFC.target)!!.toString()

  // Parse the parameters by SHACL shape.
  val shape =
      subjectWithProperty(SHACLM.targetClass, processor)
          ?: throw ParserException.MissingProcessorArguments(processor.uri)
  val parameters =
      listObjectsOfProperty(shape, SHACLM.property)
          .toList()
          .find {
            val path = objectOfProperty(it.asResource(), SHACLM.path)?.asResource()
            return@find path == RDFC.arguments
          }
          ?.let { objectOfProperty(it.asResource(), SHACLM.node)?.asResource() }
          ?.let { parseSHACLShape(it) }
          ?: throw ParserException.MissingProcessorArguments(processor.uri)

  // Parse metadata.
  val metadata = mutableMapOf<String, String>()
  for (entry in this.listObjectsOfProperty(processor, RDFC.metadata)) {
    val literal =
        try {
          entry.asLiteral().string
        } catch (e: Exception) {
          throw ParserException.InvalidMetadata(processor.uri)
        }
    val (key, value) = literal.split(':')
    metadata[key.trim()] = value.trim()
  }

  // Get entrypoint.
  val entrypoint = objectOfProperty(processor, RDFC.entrypoint)!!.toString()

  return IRProcessor(uri, target, entrypoint, parameters, metadata)
}

private fun Model.stages(pipeline: Resource): List<IRStage> {
  return listObjectsOfProperty(pipeline, RDFC.stages).toList().map { stage ->
    val processorURI = objectOfProperty(stage.asResource(), RDF.type)!!.asResource()
    val processor = processor(processorURI)
    val arguments = objectOfProperty(stage.asResource(), RDFC.arguments)!!.asResource()
    IRStage(stage.toString(), processor, arguments(arguments, processor.parameters))
  }
}

private fun Model.dependencies(pipeline: Resource?): List<IRDependency> {
  return listObjectsOfProperty(pipeline, RDFC.dependency).toList().map { dependency ->
    IRDependency(uri = dependency.toString())
  }
}

private fun Model.pkg(pkg: Resource): IRPackage {
  Log.shared.debug("Parsing package: $pkg")

  // Get all of its properties.
  val version = objectOfProperty(pkg, RDFC.version)
  val author = objectOfProperty(pkg, RDFC.author)
  val description = objectOfProperty(pkg, RDFC.description)
  val repo = objectOfProperty(pkg, RDFC.repo)
  val license = objectOfProperty(pkg, RDFC.license)
  val processors =
      listObjectsOfProperty(pkg, RDFC.processors).toList().map { processor(it.asResource()) }
  val runners = listObjectsOfProperty(pkg, RDFC.runners).toList().map { runner(it.asResource()) }

  val prepareCollection = objectOfProperty(pkg, RDFC.prepare)
  val prepare =
      if (prepareCollection != null) {
        getCollection(prepareCollection.asResource()).map { it.toString() }
      } else {
        emptyList()
      }

  // Parse the properties to strings if required, and return the package IR.
  return IRPackage(
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

private fun Model.packages(): List<IRPackage> {
  return listSubjectsWithProperty(RDF.type, RDFC.`package`).toList().map { pkg(it) }
}

private fun Model.pipeline(pipeline: Resource): IRPipeline {
  Log.shared.debug("Parsing pipeline: $pipeline")

  return IRPipeline(
      uri = pipeline.uri,
      stages = stages(pipeline),
      dependencies = dependencies(pipeline),
  )
}

private fun Model.pipelines(): List<IRPipeline> {
  return listSubjectsWithProperty(RDF.type, RDFC.pipeline).toList().map { pipeline(it) }
}

class JenaParser(
    /** A file pointer to the pipeline configuration entrypoint. */
    files: List<File>,
) : Parser {
  /** The Apache Jena model. */
  private val model: Model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)

  init {
    // Load the RDF-Connect ontology.
    val resource = this::class.java.getResource("/pipeline.ttl")
    val config = resource!!.path!!
    this.model.read(config, "TURTLE")

    // Load the pipeline file into the parser.
    for (file in files) {
      this.model.read(file.path, "TURTLE")
    }

    // Confirm that the values loaded into the model are valid.
    this.model.validate()
  }

  override fun pipelines(): List<IRPipeline> {
    return model.pipelines()
  }

  override fun packages(): List<IRPackage> {
    return model.packages()
  }

  override fun processors(): List<IRProcessor> {
    return model.packages().map { it.processors }.flatten()
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
}
