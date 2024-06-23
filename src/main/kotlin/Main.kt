package technology.idlab

import java.io.File
import kotlin.system.exitProcess
import kotlinx.coroutines.runBlocking
import technology.idlab.parser.Parser

fun main(args: Array<String>) = runBlocking {
  /**
   * At the moment, the only argument that the runtime accepts is the path to the pipeline
   * declaration file.
   */
  if (args.size != 1) {
    println("Usage: jvm-runner <config>")
    exitProcess(0)
  }

  /**
   * We start off by parsing the configuration file. This file contains the list of processors and
   * stages that the runtime should prepare, as well as channel declarations.
   */
  val configPath = args[0]
  val config = File(configPath)
  val parser = Parser.create(config)
  val processors = parser.processors()
  val stages = parser.stages()
}
