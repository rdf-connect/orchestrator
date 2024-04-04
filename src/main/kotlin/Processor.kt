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
    private val argumentTypes: Array<Class<*>> = arrayOf(String::class.java)

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

        // Retrieve actual functions.
        val compiledClass = Class.forName(this.targetClass)
        val constructor = compiledClass.getConstructor()
        this.instance = constructor.newInstance()
        this.method = compiledClass.getMethod(this.targetMethod, *this.argumentTypes)
    }

    fun execute() {
        method.invoke(instance, *this.arguments)
    }
}
