package technology.idlab.compiler

import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal

class MemoryClassLoader : ClassLoader() {
    private val logger = createLogger()

    fun fromBytes(
        bytes: ByteArray,
        name: String,
    ): Class<*> {
        logger.info("Loading class $name")

        return try {
            defineClass(name, bytes, 0, bytes.size)
        } catch (e: ClassFormatError) {
            createLogger().fatal("Failed to load class $name")
        }
    }
}
