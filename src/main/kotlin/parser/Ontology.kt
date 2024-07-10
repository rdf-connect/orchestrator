package technology.idlab.parser

import org.apache.jena.rdf.model.RDFNode

    private const val JVM_NAMESPACE = "https://w3id.org/conn/jvm#"
    private const val NAMESPACE = "https://w3id.org/conn#"

enum class Ontology(val uri: String) {
  READER("${JVM_NAMESPACE}ChannelReader"),
  WRITER("${JVM_NAMESPACE}ChannelWriter"),
  MEMORY_READER("${JVM_NAMESPACE}MemoryChannelReader"),
  MEMORY_WRITER("${JVM_NAMESPACE}MemoryChannelWriter"),
  HTTP_READER("${NAMESPACE}HttpReaderChannel"),
  HTTP_WRITER("${NAMESPACE}HttpWriterChannel"),
  ;

  companion object {
    /** Global map of all entries prefixed by the namespace. */
    private val values = entries.associateBy { it.uri }

    /** Get the enum element associated with a given URI. */
    fun get(uri: String): Ontology? {
      return values[uri]
    }

    fun get(node: RDFNode): Ontology? {
      return get(node.asResource().uri)
    }
  }
}
