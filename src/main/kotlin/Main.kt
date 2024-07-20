package technology.idlab

import java.io.File
import kotlinx.coroutines.runBlocking
import technology.idlab.extensions.rawPath
import technology.idlab.intermediate.IRPackage
import technology.idlab.parser.Parser
import technology.idlab.util.Log
import technology.idlab.util.ManagedProcess

/**
 * Execute the preparation commands in a package. If no such commands exists, the function will
 * return immediately.
 */
internal fun prepare(pkg: IRPackage) {
  Log.shared.debug { "Preparing package: file://${pkg.directory.rawPath()}" }

  pkg.prepare?.forEach { stmt ->
    // Create processor builder.
    val builder = ProcessBuilder(stmt.split(" "))
    builder.directory(File(pkg.directory.rawPath()))
    builder.environment()["PATH"] = System.getenv("PATH")

    // Execute and await the process.
    val exitCode = ManagedProcess.from(builder).waitFor()
    if (exitCode != 0) {
      Log.shared.fatal("Failed to prepare package in ${pkg.directory.rawPath()}.")
    }
  }
}

/** Execute a pipeline at a given path. */
internal suspend fun exec(path: String) {
  Log.shared.info("Starting the RDF-Connect orchestrator.")

  // Open file pointer.
  val file =
      try {
        File(path)
      } catch (e: NullPointerException) {
        Log.shared.fatal("Pipeline file does not exist.")
      }

  // Parse said config to a IRPipeline.
  Log.shared.debug("Invoking parser.")
  val parser = Parser.using(file)

  // Parse the pipeline out of the configuration file.
  if (parser.pipelines.size != 1) {
    Log.shared.fatal("The configuration file may only contain one pipeline.")
  }

  // For each package, run the preparation command if it exists.
  parser.packages.sortedBy { it.runners.size * -1 }.forEach { prepare(it) }

  // Start the orchestrator.
  Log.shared.debug("Invoking orchestrator.")
  val pipeline = parser.pipelines[0]
  val orchestrator = Orchestrator(pipeline, parser.processors, parser.runners)
  orchestrator.exec()
}

fun main(args: Array<String>) = runBlocking {
  // Retrieve the pipeline configuration path from the CLI arguments.
  if (args.size != 1) {
    Log.shared.fatal("No pipeline file provided.")
  }

  // Load the configuration file.
  val path = args[0]

  // Forward the pipeline path to the executing function.
  exec(path)
}
