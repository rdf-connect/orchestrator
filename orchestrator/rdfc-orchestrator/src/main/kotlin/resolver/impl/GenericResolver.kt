package technology.idlab.resolver.impl

import java.io.File
import technology.idlab.rdfc.core.intermediate.IRDependency
import technology.idlab.resolver.Resolver
import technology.idlab.resolver.exception.UnresolvableException

class GenericResolver : Resolver {
  override fun resolve(dependency: IRDependency): File {
    return if (dependency.uri.startsWith("file://")) {
      LocalResolver().resolve(dependency)
    } else if (dependency.uri.startsWith("git://")) {
      GitResolver().resolve(dependency)
    } else {
      throw UnresolvableException(dependency.uri)
    }
  }
}
