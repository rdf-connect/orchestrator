package technology.idlab.parser

import java.io.File
import kotlinx.coroutines.channels.Channel
import org.apache.jena.rdf.model.RDFNode
import technology.idlab.bridge.*
import technology.idlab.extensions.*
import technology.idlab.extensions.query
import technology.idlab.extensions.readModelRecursively
import technology.idlab.logging.Log
import technology.idlab.runner.Processor
import technology.idlab.runner.ProcessorDefinition

class Parser(file: File) {
  /** RDF model which imports the base pipeline ontology. */
  private val model =
      this::class
          .java
          .getResourceAsStream("/pipeline.ttl")
          .let { it ?: Log.shared.fatal("Pipeline ontology not found") }
          .readAllBytes()
          .let {
            val f = File.createTempFile("pipeline", ".ttl")
            f.writeBytes(it)
            return@let f
          }
          .readModelRecursively()

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
    ProcessorDefinition.scan().forEach {
      // Save the processor class and URI to the map.
      processors[it.uri] = it.clazz

      // Load the ontology file into the model.
      model.read(it.ontology.inputStream(), null, "TTL")
    }
  }

  /** Parse the pipeline. */
  init {
    model.read(file.inputStream(), null, "TTL").validate()
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
    model.query("/queries/readers.sparql") {
      val subClass = it["subClass"]
      val identifier = it["reader"]
      val port: Int? = it.getOptional("port")?.asLiteral()?.int
      val type = Ontology.get(subClass)

      val reader =
          when (type) {
            Ontology.MEMORY_READER -> MemoryReader()
            Ontology.HTTP_READER -> HttpReader(port ?: 8080)
            else -> Log.shared.fatal("Reader $subClass not found")
          }

      readers[identifier] = reader
    }
  }

  /** Parse the model for writers. */
  init {
    model.query("/queries/writers.sparql") {
      val subClass = it["subClass"]
      val identifier = it["writer"]
      val endpoint: String? = it.getOptional("endpoint")?.asLiteral()?.string
      val type = Ontology.get(subClass)

      val writer =
          when (type) {
            Ontology.MEMORY_WRITER -> MemoryWriter()
            Ontology.HTTP_WRITER -> HttpWriter(endpoint ?: "http://localhost:8081")
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
