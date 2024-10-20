package technology.idlab.validator

import java.io.ByteArrayOutputStream
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import org.apache.jena.graph.Graph
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shacl.ShaclValidator
import technology.idlab.rdfc.processor.Arguments
import technology.idlab.rdfc.processor.Processor

class SHACLValidator(args: Arguments) : Processor(args) {
  /** Default values. */
  private val errorIsFatalDefault = false

  /** Arguments. */
  private val fatal: Boolean? by args
  private val incoming: ReceiveChannel<ByteArray> by args
  private val outgoing: SendChannel<ByteArray> by args
  private val report: SendChannel<ByteArray>? by args
  private val shapes: String by args

  /** Runtime fields. */
  private val shapesGraph: Graph
  private val validator = ShaclValidator.get()

  init {
    // Initialize the shape graph and validator.
    log.debug { "Loading: $shapes" }

    // Create a new model with the SHACL shapesGraph.
    val model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
    model.read(shapes, "TURTLE")

    // Assign its graph to the shapesGraph field.
    this.shapesGraph = model.graph
  }

  /** Read incoming data, validate it, and output it. */
  override suspend fun exec() {
    for (data in incoming) {
      // Parse as a model.
      val model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
      model.read(data.inputStream(), null, "TURTLE")

      // Validate the model.
      val report = validator.validate(shapesGraph, model.graph)

      if (report.conforms()) {
        log.debug { "Validation successful." }
        outgoing.send(data)
      } else {
        log.debug { "Validation failed." }
        // Write report to a string.
        val out = ByteArrayOutputStream()
        report.model.write(out, "TURTLE")
        this.report?.send(out.toByteArray())

        // Throw a fatal error if needed.
        check(!(fatal ?: errorIsFatalDefault)) { "Validation error is fatal." }
      }
    }

    // Close outgoing channels.
    outgoing.close()
    report?.close()
  }
}
