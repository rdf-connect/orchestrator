package technology.idlab

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Model
import technology.idlab.compiler.JavaCodeHandler
import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.File
import java.lang.reflect.Method
import kotlin.system.exitProcess

class Processor(
    val name: String,
    private val path: String,
    private val targetClass: String,
    private val targetMethod: String,
    private val language: String,
    private val argumentNames: List<String>,
    private val argumentTypes: List<String> = listOf("java.lang.String"),
) {
    // Runtime objects.
    private val instance: Any
    private val method: Method

    companion object {
        private val logger = createLogger()

        /**
         * Read the SPARQL query from the resources folder and return it as a string.
         */
        private fun readQuery(): String {
            val resource = object {}.javaClass.getResource("/processors.sparql")
            logger.info("Reading SPARQL query from ${resource?.path}")

            return resource?.readText()
                ?: logger.fatal("Could not read ${resource?.path}")
        }

        /**
         * Parse a query solution into a processor object.
         */
        private fun fromQuerySolution(sol: QuerySolution): Processor {
            val name = sol["processor"].toString()
            val file = sol["file"].toString().drop(7)
            val targetClass = sol["class"].toString()
            val targetMethod = sol["method"].toString()
            val language = sol["language"].toString()

            // Check language constraints.
            if (language != "java") {
                println("ERROR: Unsupported language : \"${language}\".")
                exitProcess(-1)
            }

            // Parse the argument types.
            val argumentNamesShuffled = sol["names"].toString().split(";")
            val argumentIndices =
                sol["indices"].toString().split(
                    ";",
                ).map { it.toInt() }
            val argumentNames = MutableList(argumentNamesShuffled.size) { "" }
            for (i in argumentIndices.indices) {
                argumentNames[argumentIndices[i]] = argumentNamesShuffled[i]
            }

            return Processor(
                name,
                file,
                targetClass,
                targetMethod,
                language,
                argumentNames,
            )
        }

        /**
         * Parse an RDF model into a list of processors.
         */
        fun fromModel(model: Model): List<Processor> {
            // Parse query from file.
            val query = QueryFactory.create(readQuery())
            val result: MutableList<Processor> = mutableListOf()

            // Go over the resulting solutions and initialize a processor
            // object.
            logger.info("Executing SPARQL query against model")
            QueryExecutionFactory.create(query, model).use {
                val results = it.execSelect()
                while (results.hasNext()) {
                    val solution = results.nextSolution()
                    result.add(fromQuerySolution(solution))
                }
            }

            return result
        }
    }

    init {
        // Parse the source code.
        val file = File(path)
        JavaCodeHandler().compile(file)

        // Read the resulting compiled class.
        val clazz = JavaCodeHandler().load(targetClass)
        val arguments = JavaCodeHandler().mapToType(argumentTypes)
        this.instance = JavaCodeHandler().createInstance(clazz)
        this.method = clazz.getMethod(this.targetMethod, *arguments)
    }

    /**
     * Execute the processor synchronously. May not return.
     */
    fun executeSync(arguments: List<Any>) {
        method.invoke(instance, *arguments.toTypedArray())
    }
}
