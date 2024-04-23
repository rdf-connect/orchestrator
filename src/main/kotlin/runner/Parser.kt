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
import java.io.File

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
        // Parse the Turtle file as a model.
        model.read(file.absolutePath, "TURTLE")
        graph = model.graph

        // Validate it using SHACL.
        logger.info("Validating configuration using SHACL")
        val report: ValidationReport =
            ShaclValidator.get().validate(
                graph,
                graph,
            )

        if (!report.conforms()) {
            logger.fatal("Configuration does not conform to SHACL rules")
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

    fun getStages(): List<Stage> {
        val processors = getProcessors()
        return listOf(Stage(processors[0], listOf("JVM Runner")))
    }
}
