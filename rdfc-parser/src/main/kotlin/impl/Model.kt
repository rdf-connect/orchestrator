package technology.idlab.rdfc.parser.impl

import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Property
import org.apache.jena.rdf.model.RDFNode
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.ValidationReport
import org.apache.jena.vocabulary.RDF

/**
 * Run a model through a SHACL validator which is defined by the shapes within the model itself.
 *
 * @return The validation report.
 */
fun Model.validate(): ValidationReport {
  val clone = ModelFactory.createDefaultModel().add(this)
  return ShaclValidator.get().validate(this.graph, clone.graph)
}

/**
 * Return the first object which corresponds to a subject and predicate. Returns null if not found.
 *
 * @param resource The subject of the query.
 * @param property The predicate of the query.
 * @return The first result of the query, or null if not found.
 */
@Suppress("SwallowedException")
fun Model.objectOfProperty(resource: Resource, property: Property): RDFNode? {
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
@Suppress("SwallowedException")
fun Model.subjectWithProperty(property: Property, obj: RDFNode): Resource? {
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
fun Model.getCollection(resource: Resource): List<RDFNode> {
  val first = checkNotNull(objectOfProperty(resource, RDF.first)) { "No first element" }
  val rest = checkNotNull(objectOfProperty(resource, RDF.rest)) { "No rest element" }

  return if (rest != RDF.nil) {
    listOf(first) + getCollection(rest.asResource())
  } else {
    listOf(first)
  }
}
