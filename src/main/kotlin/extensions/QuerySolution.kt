package technology.idlab.extensions

import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.RDFNode

/**
 * Get an optional value from a query solution. If the key is not found, return null instead of
 * throwing an exception.
 */
internal fun QuerySolution.getOptional(key: String): RDFNode? {
  return try {
    this[key]
  } catch (e: Exception) {
    null
  }
}
