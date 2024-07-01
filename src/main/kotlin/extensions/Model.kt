package technology.idlab.extensions

import java.io.ByteArrayOutputStream
import org.apache.jena.rdf.model.Model
import org.apache.jena.shacl.ShaclValidator
import technology.idlab.util.Log

/** Validates a model against the SHACL schema defined inside the model itself. */
internal fun Model.validate(): Model {
  val graph = this.graph
  val report = ShaclValidator.get().validate(graph, graph)

  // Exit if the validation failed.
  if (!report.conforms()) {
    val out = ByteArrayOutputStream()
    report.model.write(out, "TURTLE")
    Log.shared.fatal("Validation failed\n$out")
  }

  return this
}
