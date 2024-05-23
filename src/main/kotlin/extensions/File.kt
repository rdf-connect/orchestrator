package technology.idlab.extensions

import java.io.File
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.OWL
import technology.idlab.compiler.Compiler
import technology.idlab.compiler.MemoryClassLoader
import technology.idlab.logging.Log

/**
 * Read a model from a file and recursively import all referenced ontologies based on <owl:import>
 * statements.
 */
internal fun File.readModelRecursively(): Model {
  val result = ModelFactory.createDefaultModel()

  val onthology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
  Log.shared.info("Importing file://${this.absolutePath}")
  onthology.read(this.toURI().toString(), "TURTLE")

  // Import any referenced ontologies.
  val imported: MutableSet<String> = mutableSetOf()
  val iter = onthology.listStatements(null, OWL.imports, null as Resource?)
  while (iter.hasNext()) {
    val statement = iter.nextStatement()
    val uri = statement.getObject().toString()

    // Check if we still need to import the referenced ontology.
    if (imported.contains(uri)) {
      continue
    }

    // Attempt importing the dataset.
    Log.shared.info("Importing $uri")
    try {
      result.read(uri)
    } catch (e: Exception) {
      Log.shared.fatal(e)
    }

    imported.add(uri)
  }

  // Import original onthology into the model.
  result.add(onthology)

  return result
}

/**
 * Parse a file as a JVM processor by loading the class file from disk or compiling the source code.
 */
internal fun File.loadIntoJVM(): Class<*> {
  val bytes =
      when (extension) {
        "java" -> {
          Compiler.compile(this)
        }
        "class" -> {
          readBytes()
        }
        else -> {
          Log.shared.fatal("Unsupported file extension: $extension")
        }
      }

  return MemoryClassLoader().fromBytes(bytes, nameWithoutExtension)
}
