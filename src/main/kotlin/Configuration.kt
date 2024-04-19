package technology.idlab

import kotlinx.coroutines.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.riot.Lang
import org.apache.jena.riot.RDFDataMgr
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.shacl.ValidationReport
import kotlin.system.exitProcess


class Configuration(configPath: String) {
    /** A step is simply a concrete execution of a function. */
    data class Step(val processor: Processor, val arguments: List<Any>)

    /** Processors described in the config. */
    private val processors: MutableMap<String, Processor> = mutableMapOf()

    /** Concrete functions in the pipeline, also known as steps. */
    private val steps: MutableList<Step> = mutableListOf()

    init {
        // Initialize the RDF model.
        val model = ModelFactory.createDefaultModel()
        model.read(configPath, "TURTLE")
        val graph = model.graph

        // Parse correctness using SHACL.
        val report: ValidationReport = ShaclValidator.get().validate(graph, graph)
        if (!report.conforms()) {
            println("ERROR: Configuration does not conform to SHACL rules.")
            RDFDataMgr.write(System.out, report.model, Lang.TTL);
            exitProcess(-1)
        }

        // Parse processors from the model and initialize steps.
        Processor.fromModel(model).forEach {
            processors[it.name] = it
            steps.add(Step(it, listOf("JVM Runner")))
        }
    }

    /**
     * Execute all processors in the configuration in parallel, and block until
     * all are done.
     */
    fun executeSync() = runBlocking {
        steps.map { async { it.processor.executeSync(it.arguments) } }.map { it.await() }
    }
}
