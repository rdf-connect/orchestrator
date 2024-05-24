package technology.idlab.std

import bridge.Reader
import bridge.Writer
import java.io.ByteArrayOutputStream
import java.io.File
import org.apache.jena.graph.Graph
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shacl.ShaclValidator
import technology.idlab.extensions.readModelRecursively
import technology.idlab.logging.Log
import technology.idlab.runner.Processor

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
    val shapesModel = file.readModelRecursively()
    this.shapes = shapesModel.graph
  }

  /** Read incoming data, validate it, and output it. */
  override fun exec() {
    while (true) {
      // Read incoming data.
      val res = input.readSync()
      if (res.isClosed()) {
        break
      }

      // Parse as a model.
      Log.shared.assert(model.isEmpty, "Model should be empty.")
      model.read(res.value.toString())

      // Validate the model.
      val report = validator.validate(shapes, model.graph)
      if (!report.conforms()) {
        if (printReport.orElse(printReportDefault)) {
          val out = ByteArrayOutputStream()
          report.model.write(out, "TURTLE")
          Log.shared.info(out.toString())
        }

        if (errorIsFatal.orElse(errorIsFatalDefault)) {
          Log.shared.fatal("Validation error is fatal.")
        }
      }

      // Reset model for next invocation.
      model.removeAll(null, null, null)

      // Propagate to the output.
      output.pushSync(res.value)
    }

    // Close the output.
    output.close()
  }
}
