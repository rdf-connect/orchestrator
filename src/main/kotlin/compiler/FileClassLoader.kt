package technology.idlab.compiler

import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.File

class FileClassLoader(private val directory: String) : ClassLoader() {
    private val logger = createLogger()

    override fun findClass(name: String): Class<*> {
        // Define the path to the class file.
        var path = "$directory/${name.replace('.', File.separatorChar)}.class"
        path = File(path).absolutePath

        // Open file pointer.
        val file = File(path)
        if (!file.exists()) {
            logger.fatal("Failed to read file $name")
        }

        // Read the file into a byte array.
        logger.info("Reading $path")
        val bytes = file.readBytes()

        // Define and return the class.
        return try {
            defineClass(name, bytes, 0, bytes.size)
        } catch (e: ClassFormatError) {
            logger.fatal("Failed to load class $name")
        }
    }
}
