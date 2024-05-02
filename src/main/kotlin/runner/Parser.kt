package technology.idlab.runner

import bridge.HttpReader
import bridge.Reader
import bridge.Writer
import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.channels.Channel
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.vocabulary.OWL
import technology.idlab.bridge.HttpWriter
import technology.idlab.bridge.MemoryReader
import technology.idlab.bridge.MemoryWriter
import technology.idlab.compiler.Compiler
import technology.idlab.compiler.MemoryClassLoader
import technology.idlab.logging.Log

/** Parse a solution to a Processor instance. */
private fun QuerySolution.toProcessor(): Class<Processor> {
  val file = this["file"].toString().drop(7).let { File(it) }

  // Either compile or load the file.
  val bytes =
      if (file.absolutePath.endsWith(".java")) {
        Compiler.compile(file)
      } else {
        file.readBytes()
      }

  // Load the class and return.
  return MemoryClassLoader().fromBytes(bytes, file.nameWithoutExtension).let {
    it as Class<Processor>
  }
}

/**
 * Read a model from a file and recursively import all referenced ontologies based on <owl:import>
 * statements.
 */
private fun File.readModelRecursively(): Model {
  val result = ModelFactory.createDefaultModel()

  val onthology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
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

class Parser(file: File) {
  /** An RDF model of the configuration file. */
  private val model = file.readModelRecursively().validate()

  /** Class references to the different processors. */
  private val processors: List<Class<Processor>>

  private val readers: Map<String, Reader>

  private val writers: Map<String, Writer>

  /** The stages of the pipeline. */
  val stages: List<Processor>

  /** Parse the model for processor declarations and save results as a field. */
  init {
    Log.shared.info("Parsing processors")
    val processors = mutableListOf<Class<Processor>>()

    val query =
        this.javaClass.getResource("/queries/processors.sparql")?.readText()?.let {
          QueryFactory.create(it)
        }

    // Execute the query.
    val iter = QueryExecutionFactory.create(query, model).execSelect()

    if (!iter.hasNext()) {
      Log.shared.fatal("No processors found in the configuration")
    }

    while (iter.hasNext()) {
      val solution = iter.nextSolution()
      val processor = solution.toProcessor()
      Log.shared.info("Class ${processor.name} initialised successfully")
      processors.add(processor)
    }

    this.processors = processors
  }

  /** Parse the model for readers. */
  init {
    Log.shared.info("Parsing readers")
    val readers = mutableMapOf<String, Reader>()

    val query =
        this.javaClass
            .getResource("/queries/readers.sparql")
            .let { it ?: Log.shared.fatal("Failed to read readers.sparql") }
            .readText()
            .let { QueryFactory.create(it) }

    val iter = QueryExecutionFactory.create(query, model).execSelect()

    while (iter.hasNext()) {
      val solution = iter.nextSolution()

      val subClass = solution["subClass"].toString().substringAfterLast("#")

      val identifier = solution["reader"].toString()

      val reader =
          when (subClass) {
            "MemoryChannelReader" -> MemoryReader()
            "HttpChannelReader" -> HttpReader()
            else -> Log.shared.fatal("Reader $subClass not found")
          }

      readers[identifier] = reader
    }

    this.readers = readers
  }

  /** Parse the model for writers. */
  init {
    Log.shared.info("Parsing writers")
    val writers = mutableMapOf<String, Writer>()

    val query =
        this.javaClass
            .getResource("/queries/writers.sparql")
            .let { it ?: Log.shared.fatal("Failed to read writers.sparql") }
            .readText()
            .let { QueryFactory.create(it) }

    val iter = QueryExecutionFactory.create(query, model).execSelect()

    while (iter.hasNext()) {
      val solution = iter.nextSolution()

      val subClass = solution["subClass"].toString().substringAfterLast("#")

      val identifier = solution["writer"].toString()

      val reader =
          when (subClass) {
            "MemoryChannelWriter" -> MemoryWriter()
            "HttpChannelWriter" -> HttpWriter("http://localhost:8080")
            else -> Log.shared.fatal("Reader $subClass not found")
          }

      writers[identifier] = reader
    }

    this.writers = writers
  }

  /**
   * Parse the model for bridges. Readers and writers that may be bridges in a single runner
   * instance will be bound to each other here.
   */
  init {
    Log.shared.info("Parsing bridges")

    val query =
        this.javaClass.getResource("/queries/bridges.sparql")?.readText()?.let {
          QueryFactory.create(it)
        }

    val iter = QueryExecutionFactory.create(query, model).execSelect()

    while (iter.hasNext()) {
      val solution = iter.nextSolution()

      val readerId = solution["reader"].toString()
      val writerId = solution["writer"].toString()

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
    val stages = mutableListOf<Processor>()

    // Execute the stages query.
    val query =
        this.javaClass.getResource("/queries/stages.sparql")?.readText()?.let {
          QueryFactory.create(it)
        }

    // Execute the query.
    val iter = QueryExecutionFactory.create(query, model).execSelect()

    if (!iter.hasNext()) {
      Log.shared.fatal("No processors found in the configuration")
    }

    while (iter.hasNext()) {
      val solution = iter.nextSolution()
      val stage = this.parseStage(solution)
      Log.shared.info("Stage ${stage.javaClass.name} initialised successfully")
      stages.add(stage)
    }

    this.stages = stages
  }

  /** Initialize a Processor instance based on the query solution. */
  private fun parseStage(querySolution: QuerySolution): Processor {
    val byName = processors.associateBy { it.simpleName }

    // Extract the list of arguments.
    val name = querySolution["processor"].toString().substringAfterLast("#")

    val values = querySolution["values"].toString().split(";")

    val argumentNames =
        querySolution["keys"].toString().split(";").map { it.substringAfterLast("#") }

    val types = querySolution["kinds"].toString().split(";").map { it.substringAfterLast("#") }

    // Retrieve a class instance of the Processor.
    val processor = byName[name] ?: Log.shared.fatal("Processor $name not found")
    val args = mutableMapOf<String, Any>()

    for (i in argumentNames.indices) {
      val argumentName = argumentNames[i]
      val value = values[i]
      val type = types[i]

      Log.shared.debug("$argumentName: $type = $value")

      args[argumentName] =
          when (type) {
            "integer" -> value.toInt()
            "ChannelWriter" -> writers[value] ?: Log.shared.fatal("Writer $argumentName not found")
            "ChannelReader" -> readers[value] ?: Log.shared.fatal("Reader $argumentName not found")
            else -> Log.shared.fatal("Unknown type $type")
          }
    }

    val constructor = processor.getConstructor(Map::class.java)
    return constructor.newInstance(args)
  }
}
