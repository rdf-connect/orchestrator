package technology.idlab.resolver.impl

import java.io.File
import technology.idlab.intermediate.IRDependency
import technology.idlab.resolver.Resolver
import technology.idlab.util.Log

class GenericResolver : Resolver {
  override fun resolve(dependency: IRDependency): File {
    return if (dependency.uri.startsWith("file://")) {
      LocalResolver().resolve(dependency)
    } else if (dependency.uri.startsWith("git://")) {
      GitResolver().resolve(dependency)
    } else {
      Log.shared.fatal("Cannot resolve dependency with URI ${dependency.uri}")
    }
  }
}
