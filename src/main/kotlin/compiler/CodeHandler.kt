package technology.idlab.compiler

import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.File

abstract class CodeHandler {
    protected val outputDirectory = "out"
    protected val logger = createLogger()
    private val classLoader: ClassLoader = FileClassLoader(outputDirectory)

    /**
     * Compile a single file using the language's compiler and write it to the
     * output directory.
     */
    abstract fun compile(file: File)

    /**
     * Load a class with specific name as a variable.
     */
    fun load(name: String): Class<*> {
        return classLoader.loadClass(name)
    }

    /**
     * Initialize a class using its default constructor.
     */
    fun createInstance(clazz: Class<*>): Any {
        val constructor =
            try {
                clazz.getConstructor()
            } catch (e: NoSuchMethodException) {
                logger.fatal("Could not find constructor for ${clazz.name}")
            }

        return try {
            constructor.newInstance()
        } catch (e: Exception) {
            logger.fatal("Could not instantiate ${clazz.name}")
        }
    }

    /**
     * Given a list of strings, map them to their respective classes.
     */
    fun mapToType(input: List<String>): Array<Class<*>> {
        return input.map {
            Class.forName(it)
        }.toTypedArray()
    }
}
