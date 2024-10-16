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
    val packageDirectory = initPackagesDirectory()
    val targetPath = packageDirectory.resolve(dependency.directory()).toPath()
    val sourcePath = Path(dependency.uri.removePrefix("file://"))
    val targetFile = targetPath.toFile()
    val index = targetPath.resolve("index.ttl").toFile()

    // If the target file does not exist, create a symbolic link.
    if (!targetFile.exists()) {
      targetPath.createSymbolicLinkPointingTo(sourcePath)
      return index
    }

    // File must be a symbolic link.
    check(targetPath.isSymbolicLink()) { "The file already exists and is not a symbolic link." }

    // The link must be correct.
    check(targetPath.readSymbolicLink() == sourcePath) {
      "The symbolic link does not point to the correct file."
    }

    // Don't do anything if the link is correct.
    return index
  }
}
