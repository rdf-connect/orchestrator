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

private fun Model.query(resource: String, func: (QuerySolution) -> Unit) {
  val query =
      object {}
          .javaClass
          .getResource(resource)
          .let { it ?: Log.shared.fatal("Failed to read $resource") }
          .readText()
          .let { QueryFactory.create(it) }

  val iter = QueryExecutionFactory.create(query, this).execSelect()

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

class Parser(file: File) {
  /** An RDF model of the configuration file. */
  private val model = file.readModelRecursively().validate()

  /** Class references to the different processors. */
  private val processors: MutableMap<String, Class<Processor>> = mutableMapOf()

  /** A list of all the readers in the model. */
  private val readers: MutableMap<String, Reader> = mutableMapOf()

  /** A list of all the writers in the model. */
  private val writers: MutableMap<String, Writer> = mutableMapOf()

  /** The stages of the pipeline. */
  private val stages: MutableList<Processor> = mutableListOf()

  /** Parse the model for processor declarations and save results as a field. */
  init {
    Log.shared.info("Parsing processors")

    model.query("/queries/processors.sparql") {
      val path = it["file"].toString().drop(7)
      val sourceFile = File(path)

      val bytes =
          if (sourceFile.absolutePath.endsWith(".java")) {
            Compiler.compile(sourceFile)
          } else {
            sourceFile.readBytes()
          }

      val processor =
          MemoryClassLoader().fromBytes(bytes, sourceFile.nameWithoutExtension) as Class<Processor>
      processors[processor.simpleName] = processor
    }
  }

  /** Parse the model for readers. */
  init {
    Log.shared.info("Parsing readers")

    model.query("/queries/readers.sparql") {
      val subClass = it["subClass"].toString().substringAfterLast("#")
      val identifier = it["reader"].toString()

      val reader =
          when (subClass) {
            "MemoryChannelReader" -> MemoryReader()
            "HttpChannelReader" -> HttpReader()
            else -> Log.shared.fatal("Reader $subClass not found")
          }

      readers[identifier] = reader
    }
  }

  /** Parse the model for writers. */
  init {
    Log.shared.info("Parsing writers")

    model.query("/queries/writers.sparql") {
      val subClass = it["subClass"].toString().substringAfterLast("#")
      val identifier = it["writer"].toString()

      val writer =
          when (subClass) {
            "MemoryChannelWriter" -> MemoryWriter()
            "HttpChannelWriter" -> HttpWriter("http://localhost:8080")
            else -> Log.shared.fatal("Reader $subClass not found")
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
      val readerId = it["reader"].toString()
      val writerId = it["writer"].toString()

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

    model.query("/queries/stages.sparql") {
      val name = it["processor"].toString().substringAfterLast("#")

      val values = it["values"].toString().split(";")

      val argumentNames = it["keys"].toString().split(";").map { it.substringAfterLast("#") }

      val types = it["kinds"].toString().split(";").map { it.substringAfterLast("#") }

      // Retrieve a class instance of the Processor.
      val processor = processors[name] ?: Log.shared.fatal("Processor $name not found")
      val args = mutableMapOf<String, Any>()

      for (i in argumentNames.indices) {
        val argumentName = argumentNames[i]
        val value = values[i]
        val type = types[i]

        Log.shared.debug("$argumentName: $type = $value")

        args[argumentName] =
            when (type) {
              "integer" -> value.toInt()
              "ChannelWriter" ->
                  writers[value] ?: Log.shared.fatal("Writer $argumentName not found")
              "ChannelReader" ->
                  readers[value] ?: Log.shared.fatal("Reader $argumentName not found")
              else -> Log.shared.fatal("Unknown type $type")
            }
      }

      val constructor = processor.getConstructor(Map::class.java)
      val instance = constructor.newInstance(args)
      this.stages.add(instance)
    }
  }

  fun getStages(): List<Processor> {
    return stages
  }
}
