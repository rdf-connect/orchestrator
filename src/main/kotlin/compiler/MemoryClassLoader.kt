package technology.idlab.compiler

import technology.idlab.logging.Log

class MemoryClassLoader : ClassLoader() {
    fun fromBytes(bytes: ByteArray, name: String): Class<*> {
        Log.shared.info("Loading class $name")

        return try {
            defineClass(name, bytes, 0, bytes.size)
        } catch (e: ClassFormatError) {
            Log.shared.fatal("Failed to load class $name")
        }
    }
}
