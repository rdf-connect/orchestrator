package technology.idlab.resolver.impl

import java.io.File
import technology.idlab.intermediate.IRDependency
import technology.idlab.resolver.Resolver
import technology.idlab.resolver.ResolverException.UnknownDependencyType

class GenericResolver : Resolver {
  override fun resolve(dependency: IRDependency): File {
    return if (dependency.uri.startsWith("file://")) {
      LocalResolver().resolve(dependency)
    } else if (dependency.uri.startsWith("git://")) {
      GitResolver().resolve(dependency)
    } else {
      throw UnknownDependencyType(dependency.uri)
    }
  }
}
