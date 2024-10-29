package technology.idlab.rdfc.orchestrator.resolver

import java.nio.file.Files
import kotlin.test.Test
import technology.idlab.rdfc.core.intermediate.IRDependency

abstract class ResolverTest {
  /** The URI of a dependency which must be handled successfully. */
  abstract val uri: String

  /** The concrete resolver to be tested. */
  abstract val resolver: Resolver

  @Test
  fun success() {
    val dependency = IRDependency(uri)

    // Determine the target directory of the dependency. If it exists, delete it.
    val directory = resolver.directoryOf(dependency)
    if (directory.exists()) {
      val path = directory.toPath()
      if (Files.isSymbolicLink(path)) {
        Files.delete(path)
      } else {
        directory.deleteRecursively()
      }
    }

    // Resolve the dependency.
    val index = resolver.resolve(dependency)

    // Check if the index file has been created.
    assert(index.exists())
  }
}
