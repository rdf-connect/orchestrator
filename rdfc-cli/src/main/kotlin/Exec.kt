package technology.idlab.rdfc.cli

import java.io.File
import technology.idlab.rdfc.core.log.Log
import technology.idlab.rdfc.orchestrator.orchestrator.impl.SimpleOrchestrator
import technology.idlab.rdfc.orchestrator.resolver.impl.GenericResolver
import technology.idlab.rdfc.parser.impl.JenaParser

/**
 * Execute a pipeline at a given path.
 *
 * @param path The path to the pipeline configuration file.
 */
internal suspend fun exec(path: String) {
  Log.shared.debug { "Executing pipeline at $path" }

  // Parse the configuration file.
  val config = File(path)
  val rootParser = JenaParser(listOf(config))

  // Get dependencies.
  val dependencies = rootParser.dependencies()
  val indexFiles = dependencies.map { GenericResolver().resolve(it) }

  // Check if all are resolved.
  for (indexFile in indexFiles) {
    check(indexFile.exists()) { "Dependency not resolved: ${indexFile.path}" }
  }

  // Load all index.ttl files into a new parser.
  val parser = JenaParser(listOf(listOf(config), indexFiles).flatten())

  // Start the orchestrator.
  val orchestrator = SimpleOrchestrator(parser.runners(), parser.stages())
  orchestrator.exec()

  Log.shared.info("Pipeline execution succeeded.")
}
