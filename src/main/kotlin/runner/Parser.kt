package technology.idlab.runner

import org.apache.jena.graph.Graph
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.ValidationReport
import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.OutputStream

/**
 * Parse a solution to a Processor instance.
 */
fun QuerySolution.toProcessor(): Processor {
    val logger = createLogger()

    // Retrieve the processor's attributes.
    val name = this["processor"].toString()
    val filePath = this["file"].toString().drop(7)
    val targetClass = this["class"].toString()
    val targetMethod = this["method"].toString()

    // Parse path as file.
    val file = File(filePath)
    if (!file.exists()) {
        logger.fatal("File $filePath does not exist")
    }

    // TODO: Parse argument types.
    val argumentTypes = listOf("java.lang.String")

    // Return the processor.
    return Processor(
        name,
        file,
        targetClass,
        targetMethod,
        argumentTypes,
    )
}

class Parser(file: File) {
    private val logger = createLogger()
    private val model = ModelFactory.createDefaultModel()
    private val graph: Graph

    init {
        // Load the constraints file.
        val constraints = this.javaClass.getResource("/pipeline.ttl")
        val constraintsPath = constraints?.path ?: logger.fatal(
            "Could not read ${constraints?.path}",
        )

        logger.info("Reading pipeline constraints from $constraintsPath")

        try {
            model.read(constraintsPath, "TURTLE")
        } catch (e: Exception) {
            logger.fatal("Error parsing $constraintsPath", e)
        }

        // Parse the Turtle file as a model.
        try {
            model.read(file.absolutePath, "TURTLE")
        } catch (e: Exception) {
            logger.fatal("Error parsing ${file.absolutePath}", e)
        }

        // Validate it using SHACL.
        logger.info("Validating configuration using SHACL")
        graph = model.graph
        val report: ValidationReport =
            ShaclValidator.get().validate(
                graph,
                graph,
            )

        // Write the resulting model to the console.
        val out: OutputStream = ByteArrayOutputStream()
        report.model.write(out, "TURTLE")
        logger.info("Validation completed\n$out")

        // Exit if the validation failed.
        if (!report.conforms()) {
            logger.fatal("Validation failed")
        }
    }

    fun getProcessors(): List<Processor> {
        val processors = mutableListOf<Processor>()

        val resource = this.javaClass.getResource("/processors.sparql")
        logger.info("Reading SPARQL query from ${resource?.path}")
        val processorQuery =
            resource?.readText() ?: logger.fatal(
                "Could not read ${resource?.path}",
            )
        val query = QueryFactory.create(processorQuery)

        logger.info("Executing SPARQL query against model")
        QueryExecutionFactory.create(query, model).use {
            val results = it.execSelect()
            while (results.hasNext()) {
                val solution = results.nextSolution()
                val processor = solution.toProcessor()
                processors.add(processor)
            }
        }

        return processors
    }

    fun getStages(processors: List<Processor>): List<Stage> {
        return listOf(Stage(processors[0], listOf("JVM Runner")))
    }
}
