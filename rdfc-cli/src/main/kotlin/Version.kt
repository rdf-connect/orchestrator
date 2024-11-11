package technology.idlab.rdfc.cli

import kotlin.system.exitProcess

internal fun version(): Nothing {
  println("RDF-Connect version 0.0.1")
  exitProcess(0)
}
