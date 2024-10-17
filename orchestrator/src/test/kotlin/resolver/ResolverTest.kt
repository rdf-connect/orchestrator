package resolver

import kotlin.test.Test
import technology.idlab.intermediate.IRDependency
import technology.idlab.resolver.Resolver

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
    directory.deleteRecursively()

    // Resolve the dependency.
    val index = resolver.resolve(dependency)

    // Check if the index file has been created.
    assert(index.exists())
  }
}
