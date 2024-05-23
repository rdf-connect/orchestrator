package technology.idlab.extensions

import java.time.Instant
import java.util.*
import org.apache.jena.rdf.model.RDFNode
import technology.idlab.logging.Log

/**
 * Parse an RDFNode as a literal. In contrast to the default method, this method will return the
 * exact type of the literal as described in it's datatype.
 */
internal fun RDFNode.narrowedLiteral(): Any {
  val literal = asLiteral()
  return when (literal.datatype.javaClass) {
    java.lang.Boolean::class.java -> literal.boolean
    java.lang.Byte::class.java -> literal.byte
    org.apache.jena.datatypes.xsd.XSDDateTime::class.java -> {
      val value = literal.string
      val instant = Instant.parse(value)
      Date.from(instant)
    }
    java.lang.Double::class.java -> literal.double
    java.lang.Float::class.java -> literal.float
    java.lang.Long::class.java -> literal.long
    java.lang.Integer::class.java -> literal.int
    java.lang.String::class.java -> literal.string
    else -> Log.shared.info("Unsupported data type: ${literal.datatype}")
  }
}
