package technology.idlab

import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import technology.idlab.intermediate.IRPackage
import technology.idlab.log.Log
import technology.idlab.orchestrator.impl.SimpleOrchestrator
import technology.idlab.parser.impl.JenaParser
import technology.idlab.process.ProcessManager
import technology.idlab.resolver.impl.GenericResolver

/**
 * Resolve, prepare and install all dependencies in the configuration file.
 *
 * @param path The path to the configuration file.
 * @throws CommandException If a preparation command fails.
 * @throws ConfigurationException If the configuration is invalid.
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
      if (exitCode != 0) {
        throw CommandException(stmt, exitCode)
      }
    }
  }
}

/**
 * Check if the configuration is valid.
 *
 * @param path The path to the configuration file.
 * @throws ConfigurationException If the configuration is invalid.
 */
internal fun check(path: String) {
  // Open file.
  val file = File(path)

  // Must exist.
  if (!file.exists()) {
    throw NoConfigurationFoundException()
  }

  // Must be a file.
  if (file.isFile) {
    throw InvalidConfigurationException()
  }

  // Cannot be empty.
  if (file.length() == 0L) {
    throw InvalidConfigurationException()
  }

  // Parse said config to a IRPipeline.
  val parser = JenaParser(listOf(file))

  // There must be at least one pipeline.
  val pipelines = parser.pipelines()
  if (pipelines.isEmpty()) {
    throw NoPipelineFoundException()
  }

  // Every pipeline must have one or more stages.
  for (pipeline in pipelines) {
    if (pipeline.stages.isEmpty()) {
      throw EmptyPipelineException(pipeline.uri)
    }
  }

  // All stages must use a known runner.
  val stages = pipelines.flatMap { it.stages }
  val runners = parser.runners().map { it.uri }

  for (stage in stages) {
    if (stage.processor.target !in runners) {
      throw NoSuchRunnerException(stage.processor.target)
    }
  }
}

/**
 * Execute a pipeline at a given path.
 *
 * @param path The path to the pipeline configuration file.
 * @throws ConfigurationException If the configuration is invalid.
 * @throws CommandException If a preparation command fails.
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
    if (!indexFile.exists()) {
      throw UnresolvedDependencyException(indexFile.path)
    }
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
    "check" -> check(args[1])
    "install" -> install(args[1])
    "help" -> help()
    else -> help()
  }
}
