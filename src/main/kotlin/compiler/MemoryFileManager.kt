package technology.idlab.compiler

import technology.idlab.logging.Log
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

    override fun getJavaFileForOutput(
        location: JavaFileManager.Location?,
        className: String,
        kind: JavaFileObject.Kind,
        sibling: FileObject?,
    ): JavaFileObject {
        val uriString = "string:///" +
            className.replace('.', '/') +
            kind.extension
        val uri = URI.create(uriString)

        return object : SimpleJavaFileObject(uri, kind) {
            override fun openOutputStream(): OutputStream {
                return object : ByteArrayOutputStream() {
                    override fun close() {
                        val name = className.substringAfterLast('.')
                        results[name] = this.toByteArray()
                        super.close()
                    }
                }
            }
        }
    }

    fun get(className: String): ByteArray {
        return results[className] ?: Log.shared.fatal("Class $className not found")
    }
}
