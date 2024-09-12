package technology.idlab.extensions

import java.io.ByteArrayOutputStream
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.vocabulary.RDF
import technology.idlab.util.Log

/**
 * Given an Apache Jena model, run the SHACL validation engine against itself. This means that all
 * shapes embedded in the model will be used to validate the model itself. If the validation fails,
 * the program will exit with a fatal error.
 */
internal fun Model.validate() {
  // SHACL runs against the graph, so we need to convert first. Then, simply call a new validation
  // instance and test the graph against itself.
  val graph = this.graph
  val report = ShaclValidator.get().validate(graph, graph)

  // Exit if the validation failed by logging the report.
  if (!report.conforms()) {
    val out = ByteArrayOutputStream()
    report.model.write(out, "TURTLE")
    Log.shared.fatal("Validation failed\n$out")
  }
}

/**
 * Return the first object which corresponds to a subject and predicate. Returns null if not found.
 *
 * @param resource The subject of the query.
 * @param property The predicate of the query.
 * @return The first result of the query, or null if not found.
 */
internal fun Model.objectOfProperty(resource: Resource, property: Property): RDFNode? {
  return try {
    this.listObjectsOfProperty(resource, property).next()
  } catch (e: NoSuchElementException) {
    null
  }
}

/**
 * Return the first subject which corresponds to a predicate and object. Returns null if not found.
 *
 * @param property The predicate of the query.
 * @param obj The object of the query.
 * @return The first result of the query, or null if not found.
 */
internal fun Model.subjectWithProperty(property: Property, obj: RDFNode): Resource? {
  return try {
    this.listSubjectsWithProperty(property, obj).next()
  } catch (e: NoSuchElementException) {
    null
  }
}

/**
 * Parse a collection of RDF nodes into a list. This is a recursive function that will traverse the
 * RDF list until the end.
 *
 * @param resource The head of the RDF list.
 * @return The list of RDF nodes.
 */
internal fun Model.getCollection(resource: Resource): List<RDFNode> {
  val first =
      objectOfProperty(resource, RDF.first) ?: Log.shared.fatal("No first element: $resource")
  val rest = objectOfProperty(resource, RDF.rest) ?: Log.shared.fatal("No rest element: $resource")

  return if (rest != RDF.nil) {
    listOf(first) + getCollection(rest.asResource())
  } else {
    listOf(first)
  }
}
