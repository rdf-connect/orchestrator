package technology.idlab.intermediate

import io.ktor.http.*

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
    return uri.encodeURLParameter()
  }

  /**
   * Return the path to the index file of the dependency. Note that this file may not yet exist.
   *
   * @return The path to the index file.
   */
  fun index(): String {
    return "${directory()}/index.ttl"
  }
}
