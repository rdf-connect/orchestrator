package technology.idlab.extensions

import com.google.common.reflect.ClassPath
import java.io.File
import java.net.URLClassLoader
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.vocabulary.OWL
import org.jetbrains.kotlin.incremental.isClassFile
import org.jetbrains.kotlin.incremental.isJavaFile
import org.jetbrains.kotlin.incremental.isKotlinFile
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
  if (this.isClassFile()) {
    // Load the class file using a custom class loader.
    val loader = URLClassLoader(arrayOf(toURI().toURL()))
    val classPath = ClassPath.from(loader)
    val classes = classPath.allClasses

    // Find the class which corresponds to this file.
    for (clazz in classes) {
      // TODO: this is a hack, we should use a better way to find the class.
      if (clazz.name.endsWith(this.nameWithoutExtension)) {
        return clazz.load()
      }
    }

    Log.shared.fatal("Failed to load class ${this.nameWithoutExtension}")
  }

  if (this.isKotlinFile(listOf("kt"))) {
    val bytes = Compiler.compileKotlin(this)
    return MemoryClassLoader().fromBytes(bytes, nameWithoutExtension)
  }

  if (this.isJavaFile()) {
    val bytes = Compiler.compileJava(this)
    return MemoryClassLoader().fromBytes(bytes, nameWithoutExtension)
  }

  Log.shared.fatal("Unsupported file type: $extension")
}
