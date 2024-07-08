package technology.idlab

import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import technology.idlab.parser.Parser

fun main(args: Array<String>) = runBlocking {
  // Retrieve the pipeline configuration path from the CLI arguments.
  if (args.size != 1) {
    println("Usage: rdfc <config>")
    exitProcess(0)
  }

  // Load the configuration file.
  val path = args[0]
  val file = File(path)

  // Parse said config to a IRPipeline.
  val parser = Parser(file)

  // Parse the pipeline out of the configuration file.
  val pipeline = parser.pipelines[0]

  // From all packages, retrieve all processors.
  val processors = parser.processors

  // Start the orchestrator.
  val orchestrator = Orchestrator(pipeline, processors)
  orchestrator.exec()
}
