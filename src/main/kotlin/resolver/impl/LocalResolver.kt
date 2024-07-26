package technology.idlab.resolver.impl

import java.io.File
import technology.idlab.intermediate.IRDependency
import technology.idlab.resolver.Resolver
import technology.idlab.util.Log

/** Resolve a file on the local file system. */
class LocalResolver : Resolver {
  override fun resolve(dependency: IRDependency): File {
    Log.shared.debug("Resolving dependency: ${dependency.uri}")
    return File("${dependency.uri}/index.ttl")
  }
}
