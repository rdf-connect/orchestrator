package technology.idlab.rdfc.orchestrator.resolver.impl

import java.io.File
import org.eclipse.jgit.api.Git
import technology.idlab.rdfc.core.intermediate.IRDependency
import technology.idlab.rdfc.orchestrator.resolver.Resolver

/** Resolve a Git repository by cloning it locally and reading its configuration file. */
class GitResolver : Resolver {
  override fun resolve(dependency: IRDependency): File {
    val packages = createTargetDirectory()
    val directory = packages.resolve(dependency.directory()).toPath().toFile()
    val index = indexOf(dependency)

    // Do nothing if the dependency has already been resolved.
    if (directory.exists()) {
      return index
    }

    // Return reference to the index file.
    Git.cloneRepository()
        .setURI(dependency.uri)
        .setDirectory(directory)
        .setDepth(1) // Only clone the latest commit.
        .call()

    return index
  }
}
