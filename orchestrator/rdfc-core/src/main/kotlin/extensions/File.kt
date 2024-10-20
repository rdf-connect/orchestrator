package technology.idlab.rdfc.core.extensions

import java.io.File

/**
 * Return the raw path of a file, removing the "file:" prefix and any duplicated leading slashes.
 *
 * @return The raw path of the file as a string.
 */
fun File.rawPath(): String {
  var path = this.path.removePrefix("file:")

  // Remove all but the first leading slash.
  while (this.path.startsWith("//")) {
    path = path.removePrefix("/")
  }

  return path
}
