package technology.idlab.util

import technology.idlab.compiler.Compiler
import technology.idlab.compiler.FileClassLoader
import technology.idlab.compiler.MemoryClassLoader
import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.File
import java.lang.reflect.Method

class Reflect {
    private val logger = createLogger()

    /**
     * Parse a source of class file and load the result into memory.
     */
    fun getClassFromFile(
        file: File,
        name: String,
    ): Class<*> {
        logger.info("Loading ${file.absoluteFile}")

        // Check if compilation needs to be run at runtime.
        if (file.absolutePath.endsWith(".java")) {
            val bytes = Compiler.compile(file)
            return MemoryClassLoader().fromBytes(bytes, name)
        }

        // Load from memory and return.
        return FileClassLoader().fromFile(file, name)
    }

    /**
     * Create an instance of a class using the default constructor.
     */
    fun createInstance(clazz: Class<*>): Any {
        logger.info("Instantiating ${clazz.name}")

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
     * Retrieve a method by class, method name and argument types.
     */
    fun getMethod(
        clazz: Class<*>,
        name: String,
        args: List<String>,
    ): Method {
        val arguments =
            args.map {
                try {
                    Class.forName(it)
                } catch (e: ClassNotFoundException) {
                    logger.fatal("Could not find argument class $it")
                }
            }.toTypedArray()

        logger.info(
            "Retrieving method $name with arguments ${args.joinToString()}",
        )

        return try {
            clazz.getMethod(name, *arguments)
        } catch (e: NoSuchMethodException) {
            logger.fatal("Could not find method $name in ${clazz.name}")
        }
    }
}
