package technology.idlab.resolver.impl

import java.io.File
import kotlin.io.path.Path
import kotlin.io.path.createSymbolicLinkPointingTo
import kotlin.io.path.isSymbolicLink
import kotlin.io.path.readSymbolicLink
import technology.idlab.intermediate.IRDependency
import technology.idlab.resolver.Resolver

/** Resolve a file on the local file system. */
class LocalResolver : Resolver {
  override fun resolve(dependency: IRDependency): File {
    val directory = createTargetDirectory()
    val target = directory.resolve(dependency.directory()).toPath()
    val source = Path(dependency.uri.removePrefix("file://"))
    val index = indexOf(dependency)

    // If the target file does not exist, create a symbolic link.
    if (!target.toFile().exists()) {
      target.createSymbolicLinkPointingTo(source)
      return index
    }

    // File must be a symbolic link.
    check(target.isSymbolicLink()) { "The file already exists and is not a symbolic link." }

    // The link must be correct.
    check(target.readSymbolicLink() == source) {
      "The symbolic link does not point to the correct file."
    }

    // Don't do anything if the link is correct.
    return index
  }
}
