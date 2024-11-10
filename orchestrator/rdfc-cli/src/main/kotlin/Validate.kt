package technology.idlab.rdfc.cli

import technology.idlab.rdfc.core.log.Log
import java.io.File
import technology.idlab.rdfc.parser.impl.JenaParser

/**
 * Check if the configuration is valid.
 *
 * @param path The path to the configuration file.
 */
internal fun validate(path: String) {
  Log.shared.debug { "Validating configuration at $path" }

  val file = File(path)

  // The file must exist and contain data.
  check(file.exists()) { "No configuration file found at path: $path" }
  check(file.isFile) { "Configuration file must be a file." }
  check(file.length() > 0L) { "Configuration file cannot be empty." }

  // Parse said config to a IRPipeline.
  val parser = JenaParser(listOf(file))
  val pipelines = parser.pipelines()
  check(pipelines.isNotEmpty()) { "No pipelines found in configuration." }

  // Every pipeline must have one or more stages.
  for (pipeline in pipelines) {
    check(pipeline.stages.isNotEmpty()) { "Pipeline must contain one or more stages." }
  }

  // All stages must use a known runner.
  val stages = pipelines.flatMap { it.stages }
  val runners = parser.runners().map { it.uri }

  for (stage in stages) {
    check(stage.processor.target in runners) {
      "No runner found for stage: ${stage.processor.target}"
    }
  }
}
