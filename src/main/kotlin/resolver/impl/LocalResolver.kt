package technology.idlab.resolver.impl

import java.io.File
import technology.idlab.parser.intermediate.IRDependency
import technology.idlab.resolver.Resolver

/** Resolve a file on the local file system. */
class LocalResolver : Resolver() {
  override fun resolve(dependency: IRDependency): File {
    return File("${dependency.uri}/index.ttl")
  }
}
