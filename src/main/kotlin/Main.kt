package technology.idlab

import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import technology.idlab.parser.Parser

fun main(args: Array<String>) = runBlocking {
  // Parse arguments.
  if (args.size != 1) {
    println("Usage: jvm-runner <config>")
    exitProcess(0)
  }

  // Configuration.
  val configPath = args[0]
  val config = File(configPath)
  val parser = Parser.create(config)

  // Initialize the processors.
  val processors = parser.processors()

  // Initialize the stages.
  val stages = parser.stages()
}
