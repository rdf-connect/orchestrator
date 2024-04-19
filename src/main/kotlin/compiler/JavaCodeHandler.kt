package technology.idlab.compiler

import technology.idlab.logging.fatal
import java.io.File
import java.io.PrintWriter
import javax.tools.JavaCompiler
import javax.tools.ToolProvider

class JavaCodeHandler : CodeHandler() {
    private val compiler: JavaCompiler =
        ToolProvider.getSystemJavaCompiler() ?: logger.fatal(
            "No Java compiler found.",
        )
    private val fileManager = compiler.getStandardFileManager(null, null, null)

    override fun compile(file: File) {
        val templateUrl =
            this::class.java.getResource(
                "/Template.java",
            ) ?: logger.fatal("Could not find template in resources")
        val template = File(templateUrl.toURI())
        logger.info("Using template $template")

        val compilationUnits =
            fileManager.getJavaFileObjectsFromFiles(
                listOf(template, file),
            )
        val options = listOf("-d", outputDirectory)
        val task =
            compiler.getTask(
                PrintWriter(System.out),
                fileManager,
                null,
                options,
                null,
                compilationUnits,
            )

        // Execute compilation.
        logger.info("Compiling $file")
        val success = task.call()
        if (!success) {
            logger.fatal("Compilation of $file failed")
        }
        logger.info("Compilation of $file succeeded")
    }
}
