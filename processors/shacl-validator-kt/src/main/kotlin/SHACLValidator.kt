package technology.idlab.validator

import java.io.ByteArrayOutputStream
import java.io.File
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.apache.jena.graph.Graph
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RiotException
import org.apache.jena.shacl.ShaclValidator
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor
import technology.idlab.util.Log

class SHACLValidator(args: Arguments) : Processor(args) {
  /** Default values. */
  private val errorIsFatalDefault = false

  /** Arguments. */
  private val fatal: Boolean? = arguments["validation_is_fatal"]
  private val incoming: ReceiveChannel<ByteArray> = arguments["incoming"]
  private val outgoing: SendChannel<ByteArray> = arguments["outgoing"]
  private val report: SendChannel<ByteArray>? = arguments["report"]
  private val path: String = arguments["shapes"]

  /** Runtime fields. */
  private val shapes: Graph
  private val validator = ShaclValidator.get()

  init {
    // Initialize the shape graph and validator.
    Log.shared.debug { "Loading: $path" }
    val file = File(path)

    // Create a new model with the SHACL shapes.
    val model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
    try {
      model.read(file.toURI().toString(), "TURTLE")
    } catch (e: RiotException) {
      Log.shared.fatal("Failed to read SHACL shapes from file://$path")
    }

    // Assign its graph to the shapes field.
    this.shapes = model.graph
  }

  /** Read incoming data, validate it, and output it. */
  override suspend fun exec() {
    for (data in incoming) {
      // Parse as a model.
      val model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)

      try {
        model.read(data.inputStream(), null, "TURTLE")
      } catch (e: RiotException) {
        Log.shared.fatal("Failed to read incoming RDF data.")
      }

      // Validate the model.
      val report = validator.validate(shapes, model.graph)

      if (report.conforms()) {
        Log.shared.debug { "Validation successful." }
        outgoing.send(data)
      } else {
        Log.shared.debug { "Validation failed." }
        // Write report to a string.
        val out = ByteArrayOutputStream()
        report.model.write(out, "TURTLE")
        this.report?.send(out.toByteArray())

        // Throw a fatal error if needed.
        if (fatal ?: errorIsFatalDefault) {
          Log.shared.fatal("Validation error is fatal.")
        }
      }
    }

    // Close outgoing channels.
    outgoing.close()
    report?.close()
  }
}
