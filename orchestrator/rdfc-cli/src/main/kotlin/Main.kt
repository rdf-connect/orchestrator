package technology.idlab.rdfc.cli

import kotlinx.coroutines.runBlocking

/** The main entry point for the RDF-Connect orchestrator. */
fun main(args: Array<String>) = runBlocking {
  // No arguments provided.
  if (args.isEmpty()) {
    help()
  }

  // Execute the chosen command.
  when (args[0]) {
    "exec" -> exec(args[1])
    "check" -> validate(args[1])
    "install" -> install(args[1])
    "help" -> help()
    else -> help()
  }
}
