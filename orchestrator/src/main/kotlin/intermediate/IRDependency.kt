package technology.idlab.intermediate

/** A dependency as listed in the configuration file. */
data class IRDependency(
    /** The URI of the dependency. */
    val uri: String
) {
  /**
   * Encode the URI as a string which can be used as a filename.
   *
   * @return The encoded URI.
   */
  fun directory(): String {
    return uri.substringAfterLast("/")
  }
}
