package technology.idlab.std

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

class RDFValidator(args: Arguments) : Processor(args) {
  /** Default values. */
  private val errorIsFatalDefault = false
  private val printReportDefault = false

  /** Arguments. */
  private val errorIsFatal: Boolean? = arguments["error_is_fatal"]
  private val printReport: Boolean? = arguments["print_report"]
  private val input: ReceiveChannel<ByteArray> = arguments["input"]
  private val output: SendChannel<ByteArray> = arguments["output"]

  /** Runtime fields. */
  private val shapes: Graph
  private val model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
  private val validator = ShaclValidator.get()

  // Initialize the shape graph and validator.
  init {
    val path: String = arguments["shapes"]
    val file = File(path)

    val shapesModel =
        try {
          model.read(file.toURI().toString(), "TURTLE")
        } catch (e: RiotException) {
          Log.shared.fatal("Failed to read SHACL shapes from file://$path")
        }

    this.shapes = shapesModel.graph
  }

  /** Read incoming data, validate it, and output it. */
  override suspend fun exec() {
    for (data in input) {
      // Parse as a model.
      try {
        model.read(data.inputStream(), null, "TURTLE")
      } catch (e: RiotException) {
        Log.shared.fatal("Failed to read incoming RDF data.")
      }

      // Validate the model.
      val report = validator.validate(shapes, model.graph)

      if (report.conforms()) {
        // Propagate to the output.
        output.send(data)
      } else {
        // Print the report if required.
        if (printReport ?: printReportDefault) {
          val out = ByteArrayOutputStream()
          report.model.write(out, "TURTLE")
          Log.shared.info(out.toString())
        }

        // Check if we can continue after an error.
        if (errorIsFatal ?: errorIsFatalDefault) {
          Log.shared.fatal("Validation error is fatal.")
        }
      }

      // Reset model for next invocation.
      model.removeAll(null, null, null)
    }
  }
}
