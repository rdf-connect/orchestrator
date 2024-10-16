package technology.idlab.intermediate

/**
 * Dependency of a pipeline.
 *
 * @param uri The URI of the dependency.
 */
data class IRDependency(val uri: String) {
  /**
   * Encode the URI as a string which can be used as a filename.
   *
   * @return The encoded URI.
   */
  fun directory(): String {
    return uri.substringAfterLast("/")
  }
}
