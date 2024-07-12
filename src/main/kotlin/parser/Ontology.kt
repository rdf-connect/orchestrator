package technology.idlab.parser

import org.apache.jena.rdf.model.ResourceFactory.createProperty
import org.apache.jena.rdf.model.ResourceFactory.createResource

internal class RDFC {
  companion object {
    private const val NS = "https://www.rdf-connect.com/#"
    val NAMESPACE = createResource(NS)!!
    val processor = createProperty("${NS}Processor")!!
    val `package` = createProperty("${NS}Package")!!
    val stage = createProperty("${NS}stage")!!
    val channel = createProperty("${NS}Channel")!!
    val target = createProperty("${NS}target")!!
    val metadata = createProperty("${NS}metadata")!!
    val arguments = createProperty("${NS}arguments")!!
    val kotlinRunner = createResource("${NS}Kotlin")!!
    val dependency = createProperty("${NS}dependency")!!
    val version = createProperty("${NS}version")!!
    val author = createProperty("${NS}author")!!
    val description = createProperty("${NS}description")!!
    val repo = createProperty("${NS}repo")!!
    val license = createProperty("${NS}license")!!
    val prepare = createProperty("${NS}prepare")!!
    val processors = createProperty("${NS}processors")!!
    val pipeline = createProperty("${NS}Pipeline")!!
    val stages = createProperty("${NS}stages")!!
  }
}
