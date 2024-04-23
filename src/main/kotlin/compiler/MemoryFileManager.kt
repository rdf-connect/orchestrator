package technology.idlab.compiler

import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URI
import javax.tools.FileObject
import javax.tools.ForwardingJavaFileManager
import javax.tools.JavaFileManager
import javax.tools.JavaFileObject
import javax.tools.SimpleJavaFileObject

/**
 * A custom file manager which stores compiled classes in memory and does not
 * require the need to write to disk.
 */
class MemoryFileManager(
    fileManager: JavaFileManager,
) : ForwardingJavaFileManager<JavaFileManager>(fileManager) {
    private val results: MutableMap<String, ByteArray> = HashMap()
    private val logger = createLogger()

    override fun getJavaFileForOutput(
        location: JavaFileManager.Location?,
        className: String,
        kind: JavaFileObject.Kind,
        sibling: FileObject?,
    ): JavaFileObject {
        val uri =
            URI.create(
                "string:///" + className.replace('.', '/') + kind.extension,
            )

        return object : SimpleJavaFileObject(uri, kind) {
            override fun openOutputStream(): OutputStream {
                return object : ByteArrayOutputStream() {
                    override fun close() {
                        results[className] = this.toByteArray()
                        super.close()
                    }
                }
            }
        }
    }

    fun get(className: String): ByteArray {
        logger.info("Retrieving $className")
        return results[className] ?: logger.fatal("Class $className not found")
    }
}
