package technology.idlab.runner

import bridge.HttpReader
import bridge.Reader
import bridge.Writer
import java.io.ByteArrayOutputStream
import java.io.File
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.channels.Channel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.query.ParameterizedSparqlString
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.vocabulary.OWL
import technology.idlab.bridge.HttpWriter
import technology.idlab.bridge.MemoryReader
import technology.idlab.bridge.MemoryWriter
import technology.idlab.compiler.Compiler
import technology.idlab.compiler.MemoryClassLoader
import technology.idlab.logging.Log

/**
 * Read a model from a file and recursively import all referenced ontologies based on <owl:import>
 * statements.
 */
private fun File.readModelRecursively(): Model {
  val result = ModelFactory.createDefaultModel()

  val onthology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
  Log.shared.info("Importing file://${this.absolutePath}")
  onthology.read(this.toURI().toString(), "TURTLE")

  // Import any referenced ontologies.
  val imported: MutableSet<String> = mutableSetOf()
  val iter = onthology.listStatements(null, OWL.imports, null as Resource?)
  while (iter.hasNext()) {
    val statement = iter.nextStatement()
    val uri = statement.getObject().toString()

    // Check if we still need to import the referenced ontology.
    if (imported.contains(uri)) {
      continue
    }

    // Attempt importing the dataset.
    Log.shared.info("Importing $uri")
    try {
      result.read(uri)
    } catch (e: Exception) {
      Log.shared.fatal(e)
    }

    imported.add(uri)
  }

  // Import original onthology into the model.
  result.add(onthology)

  return result
}

/**
 * Parse a file as a JVM processor by loading the class file from disk or compiling the source code.
 */
private fun File.loadIntoJVM(): Class<*> {
  val bytes =
      when (extension) {
        "java" -> {
          Compiler.compile(this)
        }
        "class" -> {
          readBytes()
        }
        else -> {
          Log.shared.fatal("Unsupported file extension: $extension")
        }
      }

  return MemoryClassLoader().fromBytes(bytes, nameWithoutExtension)
}

private fun RDFNode.narrowedLiteral(): Any {
  val literal = asLiteral()
  return when (literal.datatype.javaClass) {
    java.lang.Boolean::class.java -> literal.boolean
    java.lang.Byte::class.java -> literal.byte
    org.apache.jena.datatypes.xsd.XSDDateTime::class.java -> {
      val value = literal.string
      val instant = Instant.parse(value)
      Date.from(instant)
    }
    java.lang.Double::class.java -> literal.double
    java.lang.Float::class.java -> literal.float
    java.lang.Long::class.java -> literal.long
    java.lang.Integer::class.java -> literal.int
    java.lang.String::class.java -> literal.string
    else -> Log.shared.info("Unsupported data type: ${literal.datatype}")
  }
}

/** Execute a query as and apply a function to each solution. */
private fun Model.query(
    resource: String,
    bindings: Map<String, String> = mutableMapOf(),
    func: (QuerySolution) -> Unit
) {
  val file =
      object {}.javaClass.getResource(resource) ?: Log.shared.fatal("Failed to read $resource")

  Log.shared.info("Executing SPARQL query file://${file.path}")

  val rawQuery = file.readText()

  // Apply bindings
  val pss = ParameterizedSparqlString()
  pss.commandText = rawQuery
  bindings.forEach {
    Log.shared.debug("Binding ${it.key} to ${it.value}")
    pss.setIri(it.key, it.value)
  }

  // Create new query and execute it.
  val iter = QueryExecutionFactory.create(pss.asQuery(), this).execSelect()

  // Execute the function for each solution.
  while (iter.hasNext()) {
    val solution = iter.nextSolution()
    func(solution)
  }
}

/** Validates a model against the SHACL schema defined inside the model itself. */
private fun Model.validate(): Model {
  val graph = this.graph
  val report = ShaclValidator.get().validate(graph, graph)

  // Exit if the validation failed.
  if (!report.conforms()) {
    val out = ByteArrayOutputStream()
    report.model.write(out, "TURTLE")
    Log.shared.fatal("Validation failed\n$out")
  }

  return this
}

/**
 * Get an optional value from a query solution. If the key is not found, return null instead of
 * throwing an exception.
 */
private fun QuerySolution.getOptional(key: String): RDFNode? {
  return try {
    this[key]
  } catch (e: Exception) {
    null
  }
}

class Parser(file: File) {
  /** An RDF model of the configuration file. */
  private val model = file.readModelRecursively().validate()

  /** Class references to the different processors. */
  private val processors: MutableMap<String, Class<*>> = mutableMapOf()

  /** A list of all the readers in the model. */
  private val readers: MutableMap<RDFNode, Reader> = mutableMapOf()

  /** A list of all the writers in the model. */
  private val writers: MutableMap<RDFNode, Writer> = mutableMapOf()

  /** The stages of the pipeline. */
  private val stages: MutableList<Processor> = mutableListOf()

  /** The argument shapes of each processor. */
  private val shapes: MutableMap<String, Shape> = mutableMapOf()

  /** Parse the model for processor declarations and save results as a field. */
  init {
    Log.shared.info("Parsing processors")

    model.query("/queries/processors.sparql") {
      val uri = it["processor"].toString()
      val path = it["file"].toString().drop(7)
      val sourceFile = File(path)
      processors[uri] = sourceFile.loadIntoJVM()
    }
  }

  /** Parse the shape for each processor. */
  init {
    processors.forEach { processor ->
      Log.shared.info("Processor: ${processor.key}")
      val bindings = mutableMapOf("?target" to processor.key)
      val shape = Shape()

      model.query("/queries/shacl.sparql", bindings) {
        val name = it["path"].toString().substringAfterLast("#")
        val type = it["kind"].toString()
        val min: Int? = it.getOptional("minCount")?.asLiteral()?.int
        val max: Int? = it.getOptional("maxCount")?.asLiteral()?.int
        shape.addProperty(name, type, min, max)
      }

      shapes[processor.key] = shape
    }
  }

  /** Parse the model for readers. */
  init {
    Log.shared.info("Parsing readers")

    model.query("/queries/readers.sparql") {
      val subClass = it["subClass"]
      val identifier = it["reader"]
      val type = Ontology.get(subClass)

      val reader =
          when (type) {
            Ontology.MEMORY_READER -> MemoryReader()
            Ontology.HTTP_READER -> HttpReader()
            else -> Log.shared.fatal("Reader $subClass not found")
          }

      readers[identifier] = reader
    }
  }

  /** Parse the model for writers. */
  init {
    Log.shared.info("Parsing writers")

    model.query("/queries/writers.sparql") {
      val subClass = it["subClass"]
      val identifier = it["writer"]
      val type = Ontology.get(subClass)

      val writer =
          when (type) {
            Ontology.MEMORY_WRITER -> MemoryWriter()
            Ontology.HTTP_WRITER -> HttpWriter("http://localhost:8080")
            else -> Log.shared.fatal("Writer $subClass not found")
          }

      writers[identifier] = writer
    }
  }

  /**
   * Parse the model for bridges. Readers and writers that may be bridges in a single runner
   * instance will be bound to each other here.
   */
  init {
    Log.shared.info("Parsing bridges")

    model.query("/queries/bridges.sparql") {
      val readerId = it["reader"]
      val writerId = it["writer"]

      val reader = readers[readerId] ?: Log.shared.fatal("Reader $readerId not found")
      val writer = writers[writerId] ?: Log.shared.fatal("Writer $writerId not found")
      val channel = Channel<ByteArray>(1)

      if (reader is MemoryReader) {
        reader.setChannel(channel)
      } else {
        Log.shared.fatal("Reader $readerId is not a MemoryReader")
      }

      if (writer is MemoryWriter) {
        writer.setChannel(channel)
      } else {
        Log.shared.fatal("Writer $writerId is not a MemoryWriter")
      }
    }
  }

  /**
   * Parse the model for concrete stages, initializes the corresponding instances and saves the
   * result to a field.
   */
  init {
    Log.shared.info("Parsing stages")

    model.query("/queries/stages.sparql") { query ->
      val processor = query["processor"].toString()
      val stage = query["stage"].toString()

      // Set the name of the stage before executing the query.
      val bindings = mutableMapOf("?stage" to stage)
      val shape = shapes[processor] ?: Log.shared.fatal("Shape not found")
      val builder = shape.getBuilder()

      // Retrieve the arguments of the processor.
      model.query("/queries/arguments.sparql", bindings) {
        val key = it["key"].toString().substringAfterLast("#")
        val value = it["value"]

        val parsed =
            if (value.isLiteral) {
              value.narrowedLiteral()
            } else if (value.isURIResource) {
              val uri = shape.getProperty(key).type
              val runner = Ontology.get(uri)
              when (runner) {
                Ontology.READER -> readers[value] ?: Log.shared.fatal("Reader $key not found")
                Ontology.WRITER -> writers[value] ?: Log.shared.fatal("Writer $key not found")
                else -> TODO()
              }
            } else {
              Log.shared.fatal("Unsupported argument type")
            }

        builder.add(key, parsed)
      }

      val implementation =
          processors[processor] ?: Log.shared.fatal("Processor $processor not found")
      val constructor = implementation.getConstructor(Map::class.java)
      val args = builder.toMap()
      val instance = constructor.newInstance(args)
      this.stages.add(instance as Processor)
    }
  }

  fun getStages(): List<Processor> {
    return stages
  }
}
