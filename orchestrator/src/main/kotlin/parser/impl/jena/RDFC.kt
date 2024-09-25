package technology.idlab.parser.impl.jena

import org.apache.jena.rdf.model.ResourceFactory

class RDFC {
  companion object {
    private const val NS = "https://www.rdf-connect.com/#"
    val NAMESPACE = ResourceFactory.createResource(NS)!!
    val processor = ResourceFactory.createProperty("${NS}Processor")!!
    val `package` = ResourceFactory.createProperty("${NS}Package")!!
    val stage = ResourceFactory.createProperty("${NS}stage")!!
    val channel = ResourceFactory.createProperty("${NS}Channel")!!
    val target = ResourceFactory.createProperty("${NS}target")!!
    val metadata = ResourceFactory.createProperty("${NS}metadata")!!
    val arguments = ResourceFactory.createProperty("${NS}arguments")!!
    val dependency = ResourceFactory.createProperty("${NS}dependency")!!
    val version = ResourceFactory.createProperty("${NS}version")!!
    val author = ResourceFactory.createProperty("${NS}author")!!
    val description = ResourceFactory.createProperty("${NS}description")!!
    val repo = ResourceFactory.createProperty("${NS}repo")!!
    val license = ResourceFactory.createProperty("${NS}license")!!
    val prepare = ResourceFactory.createProperty("${NS}prepare")!!
    val runners = ResourceFactory.createProperty("${NS}runners")!!
    val processors = ResourceFactory.createProperty("${NS}processors")!!
    val pipeline = ResourceFactory.createProperty("${NS}Pipeline")!!
    val stages = ResourceFactory.createProperty("${NS}stages")!!
    val entrypoint = ResourceFactory.createProperty("${NS}entrypoint")!!
    val reader = ResourceFactory.createResource("${NS}Reader")!!
    val writer = ResourceFactory.createResource("${NS}Writer")!!
    val grpcRunner = ResourceFactory.createResource("${NS}GRPCRunner")!!
    val builtInRunner = ResourceFactory.createResource("${NS}BuiltInRunner")!!
    val workingDirectory = ResourceFactory.createProperty("${NS}wd")!!
  }
}
