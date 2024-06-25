package technology.idlab.std

import java.io.ByteArrayOutputStream
import java.io.File
import org.apache.jena.graph.Graph
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.RiotException
import org.apache.jena.shacl.ShaclValidator
import runner.jvm.Processor
import runner.jvm.Reader
import runner.jvm.Writer
import technology.idlab.extensions.readModelRecursively
import technology.idlab.util.Log

class RDFValidator(args: Map<String, Any>) : Processor(args) {
  /** Default values. */
  private val errorIsFatalDefault = false
  private val printReportDefault = false

  /** Arguments. */
  private val errorIsFatal = this.getOptionalArgument<Boolean>("error_is_fatal")
  private val printReport = this.getOptionalArgument<Boolean>("print_report")
  private val input = this.getArgument<Reader>("input")
  private val output = this.getArgument<Writer>("output")

  /** Runtime fields. */
  private val shapes: Graph
  private val model = ModelFactory.createDefaultModel()
  private val validator = ShaclValidator.get()

  // Initialize the shape graph and validator.
  init {
    val path = this.getArgument<String>("shapes")
    val file = File(path)

    val shapesModel =
        try {
          file.readModelRecursively()
        } catch (e: RiotException) {
          Log.shared.fatal("Failed to read SHACL shapes from file://$path")
        }

    this.shapes = shapesModel.graph
  }

  /** Read incoming data, validate it, and output it. */
  override suspend fun exec() {
    while (true) {
      // Read incoming data.
      val res = input.read()

      // Parse as a model.
      Log.shared.assert(model.isEmpty, "Model should be empty.")
      try {
        model.read(res.inputStream(), null, "TURTLE")
      } catch (e: RiotException) {
        Log.shared.fatal("Failed to read incoming RDF data.")
      }

      // Validate the model.
      val report = validator.validate(shapes, model.graph)

      if (report.conforms()) {
        // Propagate to the output.
        output.push(res)
      } else {
        // Print the report if required.
        if (printReport.orElse(printReportDefault)) {
          val out = ByteArrayOutputStream()
          report.model.write(out, "TURTLE")
          Log.shared.info(out.toString())
        }

        // Check if we can continue after an error.
        if (errorIsFatal.orElse(errorIsFatalDefault)) {
          Log.shared.fatal("Validation error is fatal.")
        }
      }

      // Reset model for next invocation.
      model.removeAll(null, null, null)
    }
  }
}
