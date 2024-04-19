package technology.idlab

import org.apache.jena.query.QueryExecutionFactory
import org.apache.jena.query.QueryFactory
import org.apache.jena.query.QuerySolution
import org.apache.jena.rdf.model.Model
import java.io.File
import java.lang.reflect.Method
import javax.tools.ToolProvider
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
        /**
         * Read the SPARQL query from the resources folder and return it as a string.
         */
        private fun readQuery(): String {
            val resource = object {}.javaClass.getResource("/processors.sparql")
            val data = resource?.readText()
            if (data == null) {
                println(
                    "ERROR: Could not read ${resource?.path}",
                )
                exitProcess(-1)
            }
            return data
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

        // Initialize the compiler.
        val compiler = ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            println("ERROR: No Java compiler found.")
            exitProcess(-1)
        }

        // Configure the compiler.
        val fileManager = compiler.getStandardFileManager(null, null, null)
        val compilationUnits =
            fileManager.getJavaFileObjectsFromFiles(
                listOf(file),
            )
        val task =
            compiler.getTask(
                null,
                fileManager,
                null,
                null,
                null,
                compilationUnits,
            )

        // Execute compilation.
        val success = task.call()
        if (!success) {
            println("ERROR: Compilation failed.")
            exitProcess(-1)
        }

        // Load the compiled class, call the constructor to create a new object.
        val compiledClass = Class.forName(this.targetClass)
        val constructor = compiledClass.getConstructor()
        this.instance = constructor.newInstance()

        // Retrieve the method based on the argument types.
        val argumentTypes: Array<Class<*>> =
            argumentTypes.map {
                Class.forName(it)
            }.toTypedArray()
        this.method = compiledClass.getMethod(this.targetMethod, *argumentTypes)
    }

    /**
     * Execute the processor synchronously. May not return.
     */
    fun executeSync(arguments: List<Any>) {
        method.invoke(instance, *arguments.toTypedArray())
    }
}
