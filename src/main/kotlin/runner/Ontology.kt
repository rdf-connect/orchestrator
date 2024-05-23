package technology.idlab.runner

import org.apache.jena.rdf.model.RDFNode

enum class Ontology(val uri: String) {
  READER("ChannelReader"),
  WRITER("ChannelWriter"),
  MEMORY_READER("MemoryChannelReader"),
  MEMORY_WRITER("MemoryChannelWriter"),
  HTTP_READER("HttpChannelReader"),
  HTTP_WRITER("HttpChannelWriter"),
  ;

  companion object {
    private const val NAMESPACE = "https://w3id.org/conn/jvm#"

    /** Global map of all entries prefixed by the namespace. */
    private val values = entries.associateBy { "$NAMESPACE${it.uri}" }

    /** Get the enum element associated with a given URI. */
    fun get(uri: String): Ontology? {
      return values[uri]
    }

    fun get(node: RDFNode): Ontology? {
      return get(node.asResource().uri)
    }
  }
}
