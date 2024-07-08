package technology.idlab.extensions

import java.io.File
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import technology.idlab.util.Log

/**
 * Read a model from a file and recursively import all referenced ontologies based on <owl:import>
 * statements.
 */
internal fun File.readModelRecursively(): Model {
  val onthology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
  Log.shared.info("Importing file://${this.absolutePath}")
  onthology.read(this.toURI().toString(), "TURTLE")
  return onthology
}
