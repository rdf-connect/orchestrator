package technology.idlab.resolver

import technology.idlab.RDFCException

/** An exception was thrown during the resolution of a dependency. */
abstract class ResolverException : RDFCException() {
  /** A file node exists on disk, but it is not a directory. */
  class NotADirectory : ResolverException()

  /**
   * An exception was thrown while interacting with the file system.
   *
   * @param cause The exception which caused the file system exception.
   */
  class FileSystem(override val cause: Exception? = null) : ResolverException()

  /**
   * The dependency type is not recognized.
   *
   * @param uri The URI of the dependency which is not recognized.
   */
  class UnknownDependencyType(val uri: String) : ResolverException()
}
