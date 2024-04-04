package technology.idlab

import javax.tools.*
import java.io.File
import java.lang.reflect.Method
import kotlin.system.exitProcess

class Processor(configPath: String) {
    // Configuration.
    private val sourcePath = "./Greeting.java"
    private val targetClass = "Greeting"
    private val targetMethod = "greet"
    private val arguments: Array<Any> = arrayOf("JVM Runner")
    private val argumentTypes: Array<String> = arrayOf("java.lang.String")

    // Runtime objects.
    private val instance: Any
    private val method: Method

    init {
        // Parse the source code.
        val file = File(configPath).resolve(sourcePath)

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
