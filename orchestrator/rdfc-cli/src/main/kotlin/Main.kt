package technology.idlab.rdfc.cli

import kotlinx.coroutines.runBlocking

/** The main entry point for the RDF-Connect orchestrator. */
fun main(args: Array<String>) = runBlocking {
  if (args.isEmpty()) {
    help()
  }

  if (args.any { it in listOf("-h", "--help") }) {
    help()
  }

  if (args.any { it in listOf("-v", "--version") }) {
    version()
  }

  if (args.size != 2) {
    help()
  }

  // Execute the chosen command.
  when (args[0]) {
    "exec" -> exec(args[1])
    "validate" -> validate(args[1])
    "install" -> install(args[1])
    else -> help()
  }
}
