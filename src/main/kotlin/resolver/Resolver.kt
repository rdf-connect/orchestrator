package technology.idlab.resolver

import java.io.File
import org.jetbrains.kotlin.incremental.createDirectory
import technology.idlab.parser.intermediate.IRDependency
import technology.idlab.resolver.impl.GitResolver
import technology.idlab.resolver.impl.LocalResolver
import technology.idlab.util.Log

/** Functional class which resolves a dependency based on its URI. */
abstract class Resolver {
  /** The target directory where dependencies should be cached. */
  private val target =
      File(System.getProperty("user.dir")).resolve("./rdfc_packages").createDirectory()

  /**
   * Given the IR of a dependency, resolve it, and return a file pointer to the dependency's
   * `index.ttl` configuration file.
   */
  abstract fun resolve(dependency: IRDependency): File

  companion object {
    fun resolve(dependency: IRDependency): File {
      Log.shared.info("Resolving dependency with URI ${dependency.uri}")

      return if (dependency.uri.startsWith("file://")) {
        LocalResolver().resolve(dependency)
      } else if (dependency.uri.startsWith("git://")) {
        GitResolver().resolve(dependency)
      } else {
        Log.shared.fatal("Cannot resolve dependency with URI ${dependency.uri}")
      }
    }
  }
}
