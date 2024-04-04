package technology.idlab

import org.apache.jena.graph.Graph
import org.apache.jena.rdf.model.ResourceFactory
import kotlin.system.exitProcess

class Query(private val graph: Graph) {
    fun predicate(subject: String, predicate: String): String {
        val subj = ResourceFactory.createResource(subject).asNode()
        val pred = ResourceFactory.createProperty("https://w3id.org/conn/jvm#${predicate}").asNode()

        val result = graph.find(subj, pred, null).toList()
        if (result.size != 1) {
            println("ERROR: Predicate \"${predicate}\" not found.")
            exitProcess(-1);
        }

        return result.first().`object`.toString()
    }
}
