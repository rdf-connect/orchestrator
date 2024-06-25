package technology.idlab.parser.impl

import java.io.File
import org.tomlj.Toml
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable
import runner.Runner
import technology.idlab.parser.Parser
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

private fun TomlTable.toIRParameter(): IRParameter {
  val typeString = this.getString("type") ?: Log.shared.fatal("No type found for parameter.")
  val presenceString =
      this.getString("presence") ?: Log.shared.fatal("No presence found for parameter.")
  val countString = this.getString("count") ?: Log.shared.fatal("No count found for parameter.")

  // Parse type.
  val type =
      when (typeString) {
        "string" -> IRParameter.Type.STRING
        "integer" -> IRParameter.Type.INT
        "float" -> IRParameter.Type.FLOAT
        "writer" -> IRParameter.Type.WRITER
        "reader" -> IRParameter.Type.READER
        "date" -> IRParameter.Type.DATE
        "boolean" -> IRParameter.Type.BOOLEAN
        else -> Log.shared.fatal("Unknown type: $typeString")
      }

  // Parse presence.
  val presence =
      when (presenceString) {
        "required" -> IRParameter.Presence.REQUIRED
        "optional" -> IRParameter.Presence.OPTIONAL
        else -> Log.shared.fatal("Unknown presence: $presenceString")
      }

  // Parse count.
  val count =
      when (countString) {
        "single" -> IRParameter.Count.SINGLE
        "list" -> IRParameter.Count.LIST
        else -> Log.shared.fatal("Unknown count: $countString")
      }

  return IRParameter(type, presence, count)
}

private fun TomlTable.toIRArguments(parameters: Map<String, IRParameter>): Map<String, IRArgument> {
  val results = mutableMapOf<String, IRArgument>()

  this.keySet().forEach { name ->
    if (this.isArray(name)) {
      val valuesArray = this.getArray(name)!!
      val values = mutableListOf<String>()

      for (i in 0 until valuesArray.size()) {
        val value = valuesArray.get(i).toString()
        values.add(value)
      }

      results[name] = IRArgument(parameters[name]!!, values)
    } else {
      val value = this.get(name).toString()
      results[name] = IRArgument(parameters[name]!!, listOf(value))
    }
  }

  return results
}

/**
 * Deserialize a TOML table into an IRProcessor.
 *
 * @param uri The URI of the processor, which needs to be explicitly provided since it is the key to
 *   the processor in the TOML table.
 */
private fun TomlTable.toIRProcessor(uri: String): IRProcessor {
  // Get runner target.
  val targetString = this.getString("target") ?: Log.shared.fatal("No target found for processor.")
  val target = Runner.Target.fromString(targetString)

  // Check if any parameters are provided.
  if (!this.contains("parameters")) {
    return IRProcessor(uri, target)
  }

  // Parse the parameters.
  val parametersArray = this.getArray("parameters")!!
  val result = mutableMapOf<String, IRParameter>()
  for (i in 0 until parametersArray.size()) {
    val table = parametersArray.getTable(i)
    val name = table.getString("name") ?: Log.shared.fatal("No name found for parameter.")
    val parameter = parametersArray.getTable(i).toIRParameter()
    result[name] = parameter
  }

  // Parse metadata.
  val metadata = mutableMapOf<String, String>()
  this.keySet().forEach {
    if (it != "parameters" && it != "target") {
      metadata[it] = this.getString(it) ?: Log.shared.fatal("No value found for metadata key: $it")
    }
  }

  return IRProcessor(uri, target, result, metadata)
}

private fun TomlTable.toIRStage(processors: Map<String, IRProcessor>, uri: String): IRStage {
  // Get processor.
  val processorURI =
      this.getString("processor") ?: Log.shared.fatal("No processor found for stage.")
  val processor = processors[processorURI] ?: Log.shared.fatal("Unknown processor: $processorURI")

  // Parse arguments.
  val arguments = this.getTable("arguments")?.toIRArguments(processor.parameters) ?: emptyMap()
  return IRStage(uri, processor, arguments)
}

class TomlParser(file: File) : Parser() {
  /* Parse the TOML file directly and only once. */
  private var toml: TomlParseResult = Toml.parse(file.inputStream())

  /* Save the processors in a handy map. */
  private val processors: Map<String, IRProcessor>

  /* Save stages as well. */
  private val stages: Map<String, IRStage>

  init {
    // Get the processors table.
    val processorsTable = toml.getTable("processors")

    // Iterate over the processor names and save them.
    val processors =
        processorsTable?.keySet()?.map { uri ->
          val table = processorsTable.getTable(uri)!!
          return@map table.toIRProcessor(uri)
        }

    this.processors = processors?.associateBy { it.uri } ?: emptyMap()
  }

  init {
    val stagesTable = toml.getTable("stages")

    // Iterate over the processor names and save them.
    val stages =
        stagesTable?.keySet()?.map { uri ->
          val table = stagesTable.getTable(uri)!!
          return@map table.toIRStage(processors, uri)
        }

    this.stages = stages?.associateBy { it.uri } ?: emptyMap()
  }

  override fun processors(): List<IRProcessor> {
    return processors.values.toList()
  }

  override fun stages(): List<IRStage> {
    return stages.values.toList()
  }
}
