package technology.idlab.rdfc.cli

import java.io.File
import technology.idlab.rdfc.core.log.Log
import technology.idlab.rdfc.core.process.ProcessManager
import technology.idlab.rdfc.intermediate.IRPackage
import technology.idlab.rdfc.orchestrator.resolver.impl.GenericResolver
import technology.idlab.rdfc.parser.impl.JenaParser

/**
 * Splits a string into parts based on spaces, but leaves substrings between apostrophes whole.
 *
 * For example: `"command arg1 arg2 'multipart argument'".splitToCommand()` is equivalent
 * to `listOf("command", "arg1", "arg2", "multipart argument")`.
 */
private fun String.splitToCommand(): List<String> {
  val input = this.trim()
  val parts = input.split("'").filter { it.isNotEmpty() }
  val result = mutableListOf<String>()

  var quoted = input.startsWith("'")
  for (part in parts) {
    // Add the part as a single string to the result if quoted, otherwise add
    // all substrings returned from a split on a space.
    if (quoted) {
      result.add(part)
    } else {
      val subParts = part.split(" ").filter { it.isNotEmpty() }
      result.addAll(subParts)
    }

    // Since we split on the apostrophe, this means that we continuously split
    // between quoted and non-quoted mode.
    quoted = !quoted
  }

  return result
}

/**
 * Resolve, prepare and install all dependencies in the configuration file.
 *
 * @param path The path to the configuration file.
 */
internal fun install(path: String) {
  Log.shared.debug { "Installing dependencies of $path" }

  // Load the list of dependencies from the configuration file.
  val rootParser = JenaParser(listOf(File(path)))
  val dependencies = rootParser.dependencies()

  // Resolve all dependencies and load their index files into a parser.
  data class PackageDirectory(val pkg: IRPackage, val index: File)

  val resolver = GenericResolver()
  val packages =
      dependencies
          .map {
            val resolved = resolver.resolve(it)
            val parser = JenaParser(listOf(resolved))
            PackageDirectory(parser.packages().single(), resolved)
          }
          .sortedBy { -1 * it.pkg.runners.count() }

  // For each package, run the preparation commands.
  for ((pkg, indexFile) in packages) {
    for (stmt in pkg.prepare) {
      // Create processor builder.
      val builder = ProcessBuilder(stmt.splitToCommand())
      builder.directory(indexFile.parentFile)
      builder.environment()["PATH"] = System.getenv("PATH")

      // Execute and await the process.
      val exitCode = ProcessManager(builder).process.waitFor()
      check(exitCode == 0) { "Command finished with non-zero exit code $exitCode: $stmt" }
    }
  }
}
