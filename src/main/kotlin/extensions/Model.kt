package technology.idlab.extensions

import java.io.ByteArrayOutputStream
import org.apache.jena.query.ParameterizedSparqlString
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Model
import org.apache.jena.shacl.ShaclValidator
import technology.idlab.logging.Log

/** Execute a query as and apply a function to each solution. */
internal fun Model.query(
    resource: String,
    bindings: Map<String, String> = mutableMapOf(),
    func: (QuerySolution) -> Unit
) {
  val file =
      object {}.javaClass.getResource(resource) ?: Log.shared.fatal("Failed to read $resource")

  Log.shared.info("Executing SPARQL query file://${file.path}")

  val rawQuery = file.readText()

  // Apply bindings
  val pss = ParameterizedSparqlString()
  pss.commandText = rawQuery
  bindings.forEach {
    Log.shared.debug("Binding ${it.key} to ${it.value}")
    pss.setIri(it.key, it.value)
  }

  // Create new query and execute it.
  val iter = QueryExecutionFactory.create(pss.asQuery(), this).execSelect()

  // Execute the function for each solution.
  while (iter.hasNext()) {
    val solution = iter.nextSolution()
    func(solution)
  }
}

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
