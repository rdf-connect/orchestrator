package technology.idlab.resolver

import java.io.File
import technology.idlab.intermediate.IRDependency

/** Functional class which resolves a dependency based on its URI. */
interface Resolver {
  /**
   * Given the IR of a dependency, resolve it, and return a file pointer to the dependency's
   * `index.ttl` configuration file.
   */
  fun resolve(dependency: IRDependency): File

  /**
   * In the current working directory, create the `rdfc_packages` directory if it doesn't exist. If
   * a file named `rdfc_packages` exists in the current directory, the program will exit with an
   * error.
   */
  fun initPackagesDirectory(): File {
    val packagesDir = File("rdfc_packages")

    // If the directory doesn't exist, create it.
    if (!packagesDir.exists()) {
      if (!packagesDir.mkdir()) {
        throw ResolverException.FileSystem()
      }
    }

    // Check if it is a directory.
    if (!packagesDir.isDirectory) {
      throw ResolverException.NotADirectory()
    }

    return packagesDir
  }
}
