package technology.idlab.extensions

import java.io.ByteArrayOutputStream
import org.apache.jena.shacl.ValidationReport

/**
 * Convert a validation report to its turtle string representation.
 *
 * @return The turtle string representation of the validation report.
 */
internal fun ValidationReport.toTurtleString(): String {
  val out = ByteArrayOutputStream()
  model.write(out, "TURTLE")
  return out.toString()
}
