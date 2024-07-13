package technology.idlab.resolver.impl

import java.io.File
import technology.idlab.intermediate.IRDependency
import technology.idlab.resolver.Resolver
import technology.idlab.util.Log

/** Resolve a Git repository by cloning it locally and reading its configuration file. */
class GitResolver : Resolver() {
  override fun resolve(dependency: IRDependency): File {
    Log.shared.debug("Resolving dependency: ${dependency.uri}")
    TODO("Not yet implemented")
  }
}
