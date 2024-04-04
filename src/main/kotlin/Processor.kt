package technology.idlab

import org.apache.jena.graph.Graph
import javax.tools.*
import java.io.File
import java.lang.reflect.Method
import kotlin.system.exitProcess

class Processor(graph: Graph, name: String) {
    // Configuration.
    private val filePath: String
    private val targetClass: String
    private val targetMethod: String
    private val language: String
    private val argumentTypes: Array<String> = arrayOf("java.lang.String")

    // Runtime arguments.
    private val arguments: Array<Any> = arrayOf("JVM Runner")

    // Runtime objects.
    private val instance: Any
    private val method: Method

    init {
        val query = Query(graph)
        val rawFilePath = query.predicate(name, "file")
        filePath = rawFilePath.slice(7..<rawFilePath.length)
        targetClass = query.predicate(name, "class").drop(1).dropLast(1)
        targetMethod = query.predicate(name, "method").drop(1).dropLast(1)
        language = query.predicate(name, "language").drop(1).dropLast(1)

        if (language != "java") {
            println("ERROR: Unsupported language : \"${language}\".")
            exitProcess(-1)
        }

        // Parse the source code.
        val file = File(filePath)

        // Initialize the compiler.
        val compiler = ToolProvider.getSystemJavaCompiler()
        if (compiler == null) {
            println("ERROR: No Java compiler found.")
            exitProcess(-1);
        }

        // Configure the compiler.
        val fileManager = compiler.getStandardFileManager(null, null, null)
        val compilationUnits = fileManager.getJavaFileObjectsFromFiles(listOf(file))
        val task = compiler.getTask(null, fileManager, null, null, null, compilationUnits)

        // Execute compilation.
        val success = task.call();
        if (!success) {
            println("ERROR: Compilation failed.")
            exitProcess(-1)
        }

        // Load the compiled class, call the constructor to create a new object.
        val compiledClass = Class.forName(this.targetClass)
        val constructor = compiledClass.getConstructor()
        this.instance = constructor.newInstance()

        // Retrieve the method based on the argument types.
        val argumentTypes: Array<Class<*>> = argumentTypes.map { Class.forName(it) }.toTypedArray()
        this.method = compiledClass.getMethod(this.targetMethod, *argumentTypes)
    }

    /**
     * Execute the processor synchronously. May not return.
     */
    fun executeSync() {
        method.invoke(instance, *this.arguments)
    }
}
