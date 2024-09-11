package technology.idlab.resolver

import java.io.File
import technology.idlab.intermediate.IRDependency
import technology.idlab.util.Log

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

    if (packagesDir.isFile()) {
      Log.shared.fatal("A file named `rdfc_packages` exists in the current directory.")
    }

    if (!packagesDir.exists()) {
      packagesDir.mkdir()
    }

    return packagesDir
  }
}
