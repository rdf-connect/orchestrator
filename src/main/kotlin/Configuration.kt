package technology.idlab

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.ValidationReport
import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal

class Configuration(configPath: String) {
    /** A step is simply a concrete execution of a function. */
    data class Step(val processor: Processor, val arguments: List<Any>)

    /** Shared logger object. */
    private val logger = createLogger();

    /** Processors described in the config. */
    private val processors: MutableMap<String, Processor> = mutableMapOf()

    /** Concrete functions in the pipeline, also known as steps. */
    private val steps: MutableList<Step> = mutableListOf()

    init {
        logger.info("Parsing configuration from $configPath to graph")

        // Initialize the RDF model.
        val model = ModelFactory.createDefaultModel()
        model.read(configPath, "TURTLE")
        val graph = model.graph

        // Parse correctness using SHACL.
        logger.info("Validating configuration using SHACL")
        val report: ValidationReport =
            ShaclValidator.get().validate(
                graph,
                graph,
            )

        if (!report.conforms()) {
            logger.fatal("Configuration does not conform to SHACL rules")
        }

        // Parse processors from the model and initialize steps.
        logger.info("Extracting processors from configuration")
        Processor.fromModel(model).forEach {
            processors[it.name] = it
            steps.add(Step(it, listOf("JVM Runner")))
        }
    }

    /**
     * Execute all processors in the configuration in parallel, and block until
     * all are done.
     */
    fun executeSync() {
        logger.info("Executing pipeline")
        runBlocking {
            steps.map {
                async { it.processor.executeSync(it.arguments) }
            }.map { it.await() }
        }
        logger.info("Pipeline executed successfully")
    }
}
