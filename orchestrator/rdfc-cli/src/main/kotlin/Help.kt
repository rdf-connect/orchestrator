package technology.idlab.rdfc.cli

import kotlin.system.exitProcess

/** Prints a help message to the console and exits the process with exit code 1. */
internal fun help(): Nothing {
  println("Usage: rdf-connect <mode> <path>")
  exitProcess(1)
}
