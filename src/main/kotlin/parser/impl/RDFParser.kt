package technology.idlab.parser.impl

import java.io.File
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import runner.Runner
import technology.idlab.extensions.query
import technology.idlab.extensions.validate
import technology.idlab.parser.Parser
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

class RDFParser(file: File) : Parser() {
  /* The pipeline config contains additional SHACL shapes. */
  private val config = File(this.javaClass.getResource("/pipeline.ttl")!!.toURI())

  /* Parse the RDF file into an Apache Jena model. */
  private val model: Model =
      ModelFactory.createDefaultModel()
          .read(config.inputStream(), null, "TURTLE")
          .read(file.inputStream(), null, "TURTLE")
          .validate()

  /* Cache the processors. */
  private val processors: Map<String, IRProcessor>

  /* Cache stages as well. */
  private val stages: Map<String, IRStage>

  init {
    val processors = mutableListOf<IRProcessor>()

    model.query("/queries/processors.sparql") {
      // Get URI
      val uri = it.get("uri").asResource().toString()

      // Get target.
      val targetString = it.get("target").asLiteral().string
      val target = Runner.Target.fromString(targetString)

      // Parse parameters.
      val parameterBindings = mapOf("?processor" to uri)
      val parameters = mutableMapOf<String, IRParameter>()
      model.query("/queries/parameters.sparql", parameterBindings) { query ->
        val path = query.get("path").asResource().toString().substringAfterLast("/")
        val datatype = query.get("datatype").asResource().toString()
        val minCount =
            try {
              query.get("minCount").asLiteral().int
            } catch (e: Exception) {
              null
            }
        val maxCount =
            try {
              query.get("maxCount").asLiteral().int
            } catch (e: Exception) {
              null
            }

        // Check if the parameter is required or optional.
        val presence =
            if (minCount != null && minCount > 0) {
              IRParameter.Presence.REQUIRED
            } else {
              IRParameter.Presence.OPTIONAL
            }

        // Check if the parameter is a list.
        val count =
            if (maxCount != null && maxCount == 1) {
              IRParameter.Count.SINGLE
            } else {
              IRParameter.Count.LIST
            }

        // Parse the datatype.
        // TODO: Check whether or not this gets compiled to a performant data structure, and not
        // a series of if-else statements, since that would have time complexity O(n^2).
        val type =
            when (datatype) {
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
              else -> Log.shared.fatal("Unknown datatype: $datatype")
            }

        val parameter = IRParameter(path, type, presence, count)
        parameters[path] = parameter
      }

      // Parse metadata.
      val metadataBuilder = mutableMapOf<String, String>()
      val metadataBindings = mapOf("?processor" to uri)
      model.query("/queries/metadata.sparql", metadataBindings) { query ->
        val key = query.get("key").asResource().toString().substringAfterLast("#")
        val value = query.get("value").asLiteral().string
        metadataBuilder[key] = value
      }
      val metadata = metadataBuilder.toMap()

      // Append as result.
      val processor = IRProcessor(uri, target, parameters, metadata)
      processors.add(processor)
    }

    this.processors = processors.associateBy { it.uri }
  }

  init {
    val stages = mutableListOf<IRStage>()

    model.query("/queries/stages.sparql") {
      // Get URI
      val uri = it.get("uri").asResource().toString()

      // Get processor target.
      val processorURI = it.get("processor").asResource().toString()
      val processor =
          processors[processorURI] ?: Log.shared.fatal("Unknown processor: $processorURI")

      // Parse arguments.
      val builder = mutableMapOf<String, MutableList<String>>()
      val bindings = mapOf("?stage" to uri)
      model.query("/queries/arguments.sparql", bindings) { query ->
        val key = query.get("key").asResource().toString().substringAfterLast("/")
        val value = query.get("value").asLiteral().string
        val args = builder.getOrPut(key) { mutableListOf() }
        args.add(value)
      }

      val arguments =
          builder.mapValues { (key, value) ->
            val parameter = processor.parameters[key]!!
            return@mapValues IRArgument(parameter, value)
          }

      // Append as result.
      val stage = IRStage(uri, processor, arguments)
      stages.add(stage)
    }

    this.stages = stages.associateBy { it.uri }
  }

  override fun processors(): List<IRProcessor> {
    return processors.values.toList()
  }

  override fun stages(): List<IRStage> {
    return stages.values.toList()
  }
}
