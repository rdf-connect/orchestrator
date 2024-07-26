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
}
