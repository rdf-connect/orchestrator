package technology.idlab.rdfc.orchestrator.resolver

import java.io.File
import technology.idlab.rdfc.core.intermediate.IRDependency

/** The name of the directory where the packages should be saved locally. */
private const val PACKAGES_DIR = "rdfc_packages"

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
  fun createTargetDirectory(): File {
    val packagesDir = File(".").resolve(PACKAGES_DIR).canonicalFile

    // If the directory doesn't exist, create it.
    if (!packagesDir.exists()) {
      check(packagesDir.mkdir()) { "Failed to create the packages directory." }

      // Create a `.gitignore` file in the directory which ignores all packages.
      val gitIgnore = packagesDir.resolve(".gitignore")
      gitIgnore.writeText("*")
    }

    check(packagesDir.isDirectory) { "The packages directory is not a directory." }
    return packagesDir
  }

  /**
   * Given a dependency, return the expected path to the directory of the dependency. Note that this
   * directory may not yet exist.
   *
   * @param dependency The dependency to resolve.
   * @return The path to the directory of the dependency.
   */
  fun directoryOf(dependency: IRDependency): File {
    val path = "${createTargetDirectory()}/${dependency.directory()}"
    return File(path).absoluteFile
  }

  /**
   * Given a dependency, return the expected path to the `index.ttl` file of the dependency. Note
   * that this file may not yet exist.
   *
   * @param dependency The dependency to resolve.
   * @return The path to the `index.ttl` file of the dependency.
   */
  fun indexOf(dependency: IRDependency): File {
    return directoryOf(dependency).resolve("index.ttl").absoluteFile
  }
}
