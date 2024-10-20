package technology.idlab

import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import technology.idlab.orchestrator.impl.SimpleOrchestrator
import technology.idlab.parser.impl.JenaParser
import technology.idlab.rdfc.core.intermediate.IRPackage
import technology.idlab.rdfc.core.log.Log
import technology.idlab.rdfc.core.process.ProcessManager
import technology.idlab.resolver.impl.GenericResolver

/**
 * Resolve, prepare and install all dependencies in the configuration file.
 *
 * @param path The path to the configuration file.
 */
internal fun install(path: String) {
  // Load the list of dependencies from the configuration file.
  val rootParser = JenaParser(listOf(File(path)))
  val dependencies = rootParser.dependencies()

  // Resolve all dependencies and load their index files into a parser.
  data class PackageDirectory(val pkg: IRPackage, val index: File)

  val resolver = GenericResolver()
  val packages =
      dependencies
          .map {
            val resolved = resolver.resolve(it)
            val parser = JenaParser(listOf(resolved))
            PackageDirectory(parser.packages().single(), resolved)
          }
          .sortedBy { -1 * it.pkg.runners.count() }

  // For each package, run the preparation commands.
  for ((pkg, indexFile) in packages) {
    for (stmt in pkg.prepare) {
      // Create processor builder.
      val builder = ProcessBuilder(stmt.split(" "))
      builder.directory(indexFile.parentFile)
      builder.environment()["PATH"] = System.getenv("PATH")

      // Execute and await the process.
      val exitCode = ProcessManager(builder).process.waitFor()
      check(exitCode == 0) { "Command finished with non-zero exit code $exitCode: $stmt" }
    }
  }
}

/**
 * Check if the configuration is valid.
 *
 * @param path The path to the configuration file.
 */
internal fun checkConfiguration(path: String) {
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

/**
 * Execute a pipeline at a given path.
 *
 * @param path The path to the pipeline configuration file.
 */
internal suspend fun exec(path: String) {
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
  val orchestrator = SimpleOrchestrator(parser)
  orchestrator.exec()

  Log.shared.info("Pipeline execution succeeded.")
}

/** Prints a help message to the console and exits the process with exit code 1. */
fun help(): Nothing {
  println("Usage: rdf-connect <mode> <path>")
  exitProcess(1)
}

/** The main entry point for the RDF-Connect orchestrator. */
fun main(args: Array<String>) = runBlocking {
  // No arguments provided.
  if (args.isEmpty()) {
    help()
  }

  // Execute the chosen command.
  when (args[0]) {
    "exec" -> exec(args[1])
    "check" -> checkConfiguration(args[1])
    "install" -> install(args[1])
    "help" -> help()
    else -> help()
  }
}
