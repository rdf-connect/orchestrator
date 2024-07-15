package technology.idlab

import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.runBlocking
import technology.idlab.extensions.rawPath
import technology.idlab.parser.Parser
import technology.idlab.util.Log

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
  parser.packages.forEach { pkg ->
    if (pkg.prepare?.isNotEmpty() == true) {
      Log.shared.info("Preparing package in ${pkg.directory.rawPath()}")
      pkg.prepare.forEach { stmt ->
        Log.shared.info("Executing preparation command: ${pkg.prepare}")

        // Create processor builder.
        val builder = ProcessBuilder(stmt.split(" "))
        builder.directory(File(pkg.directory.rawPath()))
        builder.environment()["PATH"] = System.getenv("PATH")

        // Start process.
        val process = builder.start()

        val input = thread {
          val stream = process.inputStream.bufferedReader()
          for (line in stream.lines()) {
            Log.shared.info(line)
          }
        }

        val output = thread {
          val stream = process.errorStream.bufferedReader()
          for (line in stream.lines()) {
            Log.shared.fatal(line)
          }
        }

        val exitCode =
            try {
              process.waitFor()
            } catch (e: InterruptedException) {
              Log.shared.fatal("Preparation command was interrupted due to ${e.message}")
            }

        if (exitCode != 0) {
          Log.shared.fatal("Preparation command failed with exit code $exitCode.")
        }

        // Quit listening, process is done.
        input.interrupt()
        output.interrupt()
      }
    }
  }

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
