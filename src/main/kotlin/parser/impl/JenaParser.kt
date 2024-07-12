package technology.idlab.parser.impl

import java.io.File
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.rdf.model.ResourceFactory.createProperty
import org.apache.jena.rdf.model.ResourceFactory.createResource
import org.apache.jena.shacl.vocabulary.SHACLM
import org.apache.jena.vocabulary.RDF
import runner.Runner
import technology.idlab.extensions.objectOfProperty
import technology.idlab.extensions.subjectWithProperty
import technology.idlab.extensions.validate
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRDependency
import technology.idlab.intermediate.IRPackage
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRPipeline
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.parser.Parser
import technology.idlab.resolver.Resolver
import technology.idlab.util.Log

private class RDFC {
  companion object {
    private const val NS = "https://www.rdf-connect.com/#"
    val NAMESPACE = createResource(NS)!!
    val processor = createProperty("${NS}Processor")!!
    val `package` = createProperty("${NS}Package")!!
    val stage = createProperty("${NS}stage")!!
    val channel = createProperty("${NS}Channel")!!
    val target = createProperty("${NS}target")!!
    val metadata = createProperty("${NS}metadata")!!
    val arguments = createProperty("${NS}arguments")!!
    val kotlinRunner = createResource("${NS}Kotlin")!!
    val dependency = createProperty("${NS}dependency")!!
    val version = createProperty("${NS}version")!!
    val author = createProperty("${NS}author")!!
    val description = createProperty("${NS}description")!!
    val repo = createProperty("${NS}repo")!!
    val license = createProperty("${NS}license")!!
    val prepare = createProperty("${NS}prepare")!!
    val processors = createProperty("${NS}processors")!!
    val pipeline = createProperty("${NS}Pipeline")!!
    val stages = createProperty("${NS}stages")!!
  }
}

private fun Resource.toRunnerTarget(): Runner.Target {
  return when (this) {
    RDFC.kotlinRunner -> Runner.Target.JVM
    else -> Log.shared.fatal("Unknown runner type: $this")
  }
}

/**
 * Maps a resource to an IRParameter.Type based on the URI. Note that this implementation is
 * actually quite slow, and we should probably use Apache Jena native APIs here.
 */
private fun Resource.toIRParameterType(): IRParameter.Type {
  return when (this.uri) {
    "http://www.w3.org/2001/XMLSchema#boolean" -> IRParameter.Type.BOOLEAN
    "http://www.w3.org/2001/XMLSchema#byte" -> IRParameter.Type.BYTE
    "http://www.w3.org/2001/XMLSchema#datetime" -> IRParameter.Type.DATE
    "http://www.w3.org/2001/XMLSchema#double" -> IRParameter.Type.DOUBLE
    "http://www.w3.org/2001/XMLSchema#float" -> IRParameter.Type.FLOAT
    "http://www.w3.org/2001/XMLSchema#int" -> IRParameter.Type.INT
    "http://www.w3.org/2001/XMLSchema#long" -> IRParameter.Type.LONG
    "http://www.w3.org/2001/XMLSchema#string" -> IRParameter.Type.STRING
    "http://www.rdf-connect.com/#/writer" -> IRParameter.Type.WRITER
    "http://www.rdf-connect.com/#/reader" -> IRParameter.Type.READER
    else -> Log.shared.fatal("Unknown datatype: ${this.uri}")
  }
}

/**
 * Create a mapping of String to IRParameter from a SHACL property. This is a recursive
 * implementation that will automatically parse nested classes.
 */
private fun Model.parseSHACLProperty(property: Resource): Pair<String, IRParameter> {
  // Retrieve required fields.
  val minCount = objectOfProperty(property, SHACLM.minCount)?.asLiteral()?.int
  val maxCount = objectOfProperty(property, SHACLM.maxCount)?.asLiteral()?.int
  val node = objectOfProperty(property, SHACLM.node)?.asResource()
  val datatype = objectOfProperty(property, SHACLM.datatype)?.asResource()

  // Retrieve the path of the property.
  val path =
      try {
        objectOfProperty(property, SHACLM.name)!!.asLiteral().string
      } catch (e: Exception) {
        Log.shared.fatal("SHACL property must have a name.")
      }

  // Determine the presence.
  val presence =
      if (minCount != null && minCount > 0) {
        IRParameter.Presence.REQUIRED
      } else {
        IRParameter.Presence.OPTIONAL
      }

  // Determine the count.
  val count =
      if (maxCount != null && maxCount == 1) {
        IRParameter.Count.SINGLE
      } else {
        IRParameter.Count.LIST
      }

  // Create a new parameter object.
  val parameter =
      if (datatype != null) {
        IRParameter(simple = datatype.toIRParameterType(), presence = presence, count = count)
      } else if (node != null) {
        IRParameter(complex = parseSHACLShape(node), presence = presence, count = count)
      } else {
        Log.shared.fatal("SHACL property must have either a datatype or a class.")
      }

  // Return the parameter mapped to its path.
  return Pair(path, parameter)
}

/**
 * Parse a SHACL shape into a mapping of String to IRParameter. This is a recursive implementation
 * that will automatically parse nested classes.
 */
private fun Model.parseSHACLShape(shape: Resource): Map<String, IRParameter> {
  val result = mutableMapOf<String, IRParameter>()

  for (property in listObjectsOfProperty(shape, SHACLM.property)) {
    val (key, parameter) = parseSHACLProperty(property.asResource())
    result[key] = parameter
  }

  return result
}

private fun Model.nameOfSHACLPath(path: Resource): String {
  val property =
      subjectWithProperty(SHACLM.path, path)
          ?: Log.shared.fatal("No property found for path: $path")
  return objectOfProperty(property, SHACLM.name)?.asLiteral()?.string
      ?: Log.shared.fatal("No name found for path: $path")
}

/**
 * Parse the arguments of a stage. This is a recursive implementation that will automatically parse
 * nested classes. Recursion will continue until all objects found are literals.
 */
private fun Model.parseArguments(node: Resource): Map<String, IRArgument> {
  val simple = mutableMapOf<String, MutableList<String>>()
  val complex = mutableMapOf<String, MutableList<Map<String, IRArgument>>>()

  // Go over each triple of the resource. If it is a literal, add it to the simple list. Otherwise,
  // call recursively and add it to the complex list.
  for (triple in listStatements(node, null, null as RDFNode?)) {
    val key = nameOfSHACLPath(triple.predicate)
    val value = triple.`object`

    if (value.isLiteral) {
      val list = simple.getOrPut(key) { mutableListOf() }
      list.add(value.asLiteral().string)
    } else if (value.isResource) {
      val list = complex.getOrPut(key) { mutableListOf() }
      val nested = parseArguments(value.asResource())
      list.add(nested)
    } else {
      Log.shared.fatal("Unknown RDFNode type: $value")
    }
  }

  // Combine both simple and complex mappings as a single map to IRArguments.
  return simple.mapValues { (_, value) -> IRArgument(simple = value) } +
      complex.mapValues { (_, value) -> IRArgument(complex = value) }
}

private fun Model.parseProcessor(processor: Resource): IRProcessor {
  val uri = processor.toString()

  // Determine the target runner.
  val target = objectOfProperty(processor, RDFC.target)!!.asResource().toRunnerTarget()

  // Parse the parameters by SHACL shape.
  val shape =
      subjectWithProperty(SHACLM.targetClass, processor)
          ?: Log.shared.fatal("No shape found for processor: ${processor}")
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

  return IRProcessor(uri, target, parameters, metadata)
}

private fun Model.parseStages(pipeline: Resource): List<IRStage> {
  return listObjectsOfProperty(pipeline, RDFC.stages).toList().map { stage ->
    val processor = objectOfProperty(stage.asResource(), RDF.type)!!.asResource()
    val arguments = objectOfProperty(stage.asResource(), RDFC.arguments)!!.asResource()
    IRStage(stage.toString(), processor.uri, parseArguments(arguments))
  }
}

private fun Model.parseDependencies(pipeline: Resource?): List<IRDependency> {
  return listObjectsOfProperty(pipeline, RDFC.dependency).toList().map { dependency ->
    IRDependency(uri = dependency.toString())
  }
}

private fun Model.parsePackage(pkg: Resource): IRPackage {
  // Get all of its properties.
  val version = objectOfProperty(pkg, RDFC.version)
  val author = objectOfProperty(pkg, RDFC.author)
  val description = objectOfProperty(pkg, RDFC.description)
  val repo = objectOfProperty(pkg, RDFC.repo)
  val license = objectOfProperty(pkg, RDFC.license)
  val prepare = objectOfProperty(pkg, RDFC.prepare)
  val processors =
      listObjectsOfProperty(pkg, RDFC.processors).toList().map { parseProcessor(it.asResource()) }

  // Parse the properties to strings if required, and return the package IR.
  return IRPackage(
      version = version?.toString(),
      author = author?.toString(),
      description = description.toString(),
      repo = repo.toString(),
      license = license.toString(),
      prepare = prepare.toString(),
      processors = processors,
  )
}

private fun Model.parsePackages(): List<IRPackage> {
  return listSubjectsWithProperty(RDF.type, RDFC.`package`).toList().map { parsePackage(it) }
}

private fun Model.parsePipeline(pipeline: Resource): IRPipeline {
  return IRPipeline(
      uri = pipeline.uri,
      stages = parseStages(pipeline),
      dependencies = parseDependencies(pipeline),
  )
}

private fun Model.parsePipelines(): List<IRPipeline> {
  return listSubjectsWithProperty(RDF.type, RDFC.pipeline).toList().map { parsePipeline(it) }
}

class JenaParser(file: File) : Parser() {
  /** The Apache Jena model. */
  private val model: Model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)

  /** The pipelines in the current configuration. */
  override val pipelines: List<IRPipeline>

  /** The packages in the current configuration. */
  override val packages: List<IRPackage>

  /** List of all known processors. */
  override val processors: List<IRProcessor>

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
