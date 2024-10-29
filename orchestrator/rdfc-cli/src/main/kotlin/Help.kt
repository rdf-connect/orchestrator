package technology.idlab.rdfc.cli

import kotlin.system.exitProcess

/** Prints a help message to the console and exits the process with exit code 1. */
internal fun help(): Nothing {
  println("Usage: rdf-connect <mode> <path>")
  println("\nModes:")
  println("  install         Download and prepare dependencies")
  println("  exec            Execute a pipeline")
  println("  validate        Validate a configuration file")
  println("\nOptions:")
  println("  -h, --help      Print this help message")
  println("  -v, --version   Show the version number")
  exitProcess(1)
}
