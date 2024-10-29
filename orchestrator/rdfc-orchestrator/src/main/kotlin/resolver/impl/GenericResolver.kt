package technology.idlab.rdfc.orchestrator.resolver.impl

import java.io.File
import technology.idlab.rdfc.core.intermediate.IRDependency
import technology.idlab.rdfc.orchestrator.resolver.Resolver
import technology.idlab.rdfc.orchestrator.resolver.exception.UnresolvableException

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
