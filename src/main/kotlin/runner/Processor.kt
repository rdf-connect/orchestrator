package technology.idlab.runner

import technology.idlab.util.Reflect
import java.io.File

class Processor(
    val name: String,
    file: File,
    targetClass: String,
    targetMethod: String,
    arguments: List<String>,
) {
    // Runtime objects.
    private val reflect = Reflect()
    private val clazz = reflect.getClassFromFile(file, targetClass)
    private val instance = reflect.createInstance(clazz)
    private val method = reflect.getMethod(clazz, targetMethod, arguments)

    /**
     * Execute the processor synchronously. May not return.
     */
    fun executeSync(arguments: List<Any>) {
        method.invoke(instance, *arguments.toTypedArray())
    }
}
