package technology.idlab.compiler

import technology.idlab.logging.Log
import java.io.File
import java.io.PrintWriter
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

class Compiler {
    private val compiler =
        ToolProvider.getSystemJavaCompiler() ?: Log.shared.fatal(
            "No Java compiler found.",
        )

    fun compile(file: File): ByteArray {
        Log.shared.info("Compiling file://${file.absolutePath}")

        // Prepare compilation.
        val files = listOf(file)
        val fileManager = compiler.getStandardFileManager(null, null, null)
        val results = MemoryFileManager(fileManager)
        val compilationUnits = fileManager.getJavaFileObjectsFromFiles(files)
        val diagnosticCollector = DiagnosticCollector<JavaFileObject>()

        // Create a compilation task.
        val task = compiler.getTask(
            PrintWriter(System.out),
            results,
            diagnosticCollector,
            listOf("-d", ""),
            null,
            compilationUnits,
        )

        // Execute compilation.
        val success = task.call()

        // Write diagnostics to logger.
        diagnosticCollector.diagnostics.forEach {
            Log.shared.info(it.toString())
        }

        if (!success) {
            Log.shared.fatal("ERROR: compilation failed")
        }

        return results.get(file.nameWithoutExtension)
    }

    companion object {
        private val instance = Compiler()

        fun compile(file: File): ByteArray {
            return instance.compile(file)
        }
    }
}
