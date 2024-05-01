package technology.idlab.runner

import MemoryBridge
import org.apache.jena.ontology.OntModelSpec
import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.ModelFactory
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.ShaclValidator
import org.apache.jena.vocabulary.OWL
import java.io.ByteArrayOutputStream
import java.io.File
import technology.idlab.compiler.Compiler
import technology.idlab.compiler.MemoryClassLoader
import technology.idlab.logging.Log

/**
 * Parse a solution to a runner.Processor instance.
 */
private fun QuerySolution.toProcessor(): Class<Processor> {
    val file = this["file"]
        .toString()
        .drop(7)
        .let { File(it) }

    // Either compile or load the file.
    val bytes = if (file.absolutePath.endsWith(".java")) {
        Compiler.compile(file)
    } else {
        file.readBytes()
    }

    // Load the class and return.
    return MemoryClassLoader()
        .fromBytes(bytes, file.nameWithoutExtension)
        .let { it as Class<Processor> }
}

private fun QuerySolution.toStage(processors: List<Class<Processor>>): Processor {
    val byName = processors.associateBy { it.simpleName }

    // Extract the list of arguments.
    val name = this["processor"]
        .toString()
        .substringAfterLast("#")

    val values = this["values"].toString().split(";")

    val keys = this["keys"]
        .toString()
        .split(";")
        .map { it.substringAfterLast("#") }

    val kinds = this["kinds"]
        .toString()
        .split(";")
        .map { it.substringAfterLast("#") }

    // Retrieve a class instance of the Processor.
    val processor = byName[name] ?: Log.shared.fatal("Processor $name not found")
    val args = mutableMapOf<String, Any>()

    for (i in keys.indices) {
        val key = keys[i]
        val value = values[i]
        val kind = kinds[i]

        Log.shared.debug("$key: $kind = $value")

        args[key] = when (kind) {
            "integer" -> value.toInt()
            "ChannelWriter" -> bridge
            "ChannelReader" -> bridge
            else -> Log.shared.fatal("Unknown kind $kind")
        }
    }

    val constructor = processor.getConstructor(Map::class.java)
    return constructor.newInstance(args)
}

// TODO: Create some sort of a factory.
val bridge = MemoryBridge()

class Parser(file: File) {
    private val model = ModelFactory.createDefaultModel()

    init {
        val onthology = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM)
        onthology.read(file.toURI().toString(), "TURTLE")

        // Import any referenced ontologies.
        val imported: MutableSet<String> = mutableSetOf()
        val iter = onthology.listStatements(null, OWL.imports, null as Resource?)
        while (iter.hasNext()) {
            val statement = iter.nextStatement()
            val uri = statement.getObject().toString()

            // Check if we still need to import the referenced ontology.
            if (imported.contains(uri)) {
                continue
            }

            // Attempt importing the dataset.
            Log.shared.info("Importing $uri")
            try {
                model.read(uri)
            } catch (e: Exception) {
                Log.shared.fatal(e)
            }

            imported.add(uri)
        }

        // Import original onthology into the model.
        model.add(onthology)

        // Validate using SHACL.
        val report = ShaclValidator.get().validate(model.graph, model.graph)

        // Exit if the validation failed.
        if (!report.conforms()) {
            val out = ByteArrayOutputStream()
            report.model.write(out, "TURTLE")
            Log.shared.fatal("Validation failed\n$out")
        }
    }

    private fun getProcessors(): List<Class<Processor>> {
        Log.shared.info("Parsing processors")
        val processors = mutableListOf<Class<Processor>>()
        val query = this.javaClass.getResource("/queries/processors.sparql")
            ?.readText()
            ?.let { QueryFactory.create(it) }

        // Execute the query.
        val iter = QueryExecutionFactory
            .create(query, model)
            .execSelect()

        if (!iter.hasNext()) {
            Log.shared.fatal("No processors found in the configuration")
        }

        while (iter.hasNext()) {
            val solution = iter.nextSolution()
            val processor = solution.toProcessor()
            Log.shared.info("Class ${processor.name} initialised successfully")
            processors.add(processor)
        }

        return processors
    }

    fun getStages(): List<Processor> {
        val processors = getProcessors()
        Log.shared.info("Parsing stages")

        // Execute the stages query.
        val query = this.javaClass.getResource("/queries/stages.sparql")
            ?.readText()
            ?.let { QueryFactory.create(it) }

        // Execute the query.
        val iter = QueryExecutionFactory
            .create(query, model)
            .execSelect()

        if (!iter.hasNext()) {
            Log.shared.fatal("No processors found in the configuration")
        }

        val result = mutableListOf<Processor>()

        while (iter.hasNext()) {
            val solution = iter.nextSolution()
            val stage = solution.toStage(processors)
            Log.shared.info("Stage ${stage.javaClass.name} initialised successfully")
            result.add(stage)
        }

        return result
    }
}
