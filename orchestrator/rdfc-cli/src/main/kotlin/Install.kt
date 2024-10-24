package technology.idlab.rdfc.cli

import java.io.File
import technology.idlab.parser.impl.JenaParser
import technology.idlab.rdfc.core.intermediate.IRPackage
import technology.idlab.rdfc.core.process.ProcessManager
import technology.idlab.resolver.impl.GenericResolver

/**
 * Resolve, prepare and install all dependencies in the configuration file.
 *
 * @param path The path to the configuration file.
 */
internal fun install(path: String) {
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
      val builder = ProcessBuilder(stmt.split(" "))
      builder.directory(indexFile.parentFile)
      builder.environment()["PATH"] = System.getenv("PATH")

      // Execute and await the process.
      val exitCode = ProcessManager(builder).process.waitFor()
      check(exitCode == 0) { "Command finished with non-zero exit code $exitCode: $stmt" }
    }
  }
}
