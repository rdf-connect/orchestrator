package technology.idlab.compiler

import technology.idlab.logging.createLogger
import technology.idlab.logging.fatal
import java.io.File
import javax.tools.JavaCompiler
import javax.tools.ToolProvider

class JavaCodeHandler : CodeHandler() {
    override val logger = createLogger()
    private val compiler: JavaCompiler = ToolProvider.getSystemJavaCompiler() ?: logger.fatal("No Java compiler found.")
    private val fileManager = compiler.getStandardFileManager(null, null, null)

    override fun compile(file: File) {
        val compilationUnits = fileManager.getJavaFileObjectsFromFiles(listOf(file))
        val options = listOf("-d", outputDirectory)
        val task = compiler.getTask(null, fileManager, null, options, null, compilationUnits)

        // Execute compilation.
        logger.info("Compiling $file")
        val success = task.call()
        if (!success) {
            logger.fatal("Compilation of $file failed")
        }
        logger.info("Compilation of $file succeeded")
    }
}
