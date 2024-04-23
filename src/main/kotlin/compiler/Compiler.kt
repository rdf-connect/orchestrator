package technology.idlab.compiler

import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.File
import java.io.PrintWriter
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider

class Compiler {
    private val logger = createLogger()
    private val compiler =
        ToolProvider.getSystemJavaCompiler() ?: logger.fatal(
            "No Java compiler found.",
        )

    fun compile(file: File): ByteArray {
        logger.info(file.absolutePath)

        // Prepare compilation.
        val files = listOf(file)
        val fileManager = compiler.getStandardFileManager(null, null, null)
        val results = MemoryFileManager(fileManager)
        val compilationUnits = fileManager.getJavaFileObjectsFromFiles(files)
        val diagnosticCollector = DiagnosticCollector<JavaFileObject>()

        // Create a compilation task.
        val task =
            compiler.getTask(
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
            logger.info(it.toString())
        }

        if (!success) {
            logger.fatal("Failure when compiling $file")
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
