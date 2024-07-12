package technology.idlab.resolver.impl

import java.io.File
import technology.idlab.intermediate.IRDependency
import technology.idlab.resolver.Resolver

/** Resolve a Git repository by cloning it locally and reading its configuration file. */
class GitResolver : Resolver() {
  override fun resolve(dependency: IRDependency): File {
    TODO("Not yet implemented")
  }
}
