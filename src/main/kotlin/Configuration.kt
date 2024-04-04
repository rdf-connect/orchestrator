package technology.idlab

import kotlinx.coroutines.*
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.ResourceFactory

class Configuration(configPath: String) {
    /** Processors described in the config. */
    private val processors: MutableList<Processor> = mutableListOf()

    init {
        // Initialize the RDF model.
        val model = ModelFactory.createDefaultModel()
        model.read(configPath, "TURTLE")

        // A processor must be of the following type.
        val objectResource = ResourceFactory.createResource("https://w3id.org/conn/jvm#Processor")

        // Delegate initialization to the individual processors.
        model.graph
            .find(null, null, objectResource.asNode())
            .toList()
            .forEach {
                processors.add(Processor(model.graph, it.subject.toString()))
            }
    }

    /**
     * Execute all processors in the configuration in parallel, and block until
     * all are done.
     */
    fun executeSync() = runBlocking {
        processors.map { async { it.executeSync() } }.map { it.await() }
    }
}
