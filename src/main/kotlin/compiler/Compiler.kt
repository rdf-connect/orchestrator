package technology.idlab.compiler

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import java.io.PrintWriter
import javax.tools.DiagnosticCollector
import javax.tools.JavaFileObject
import javax.tools.ToolProvider
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import technology.idlab.logging.Log

class Compiler {
  private val kotlinCompiler = K2JVMCompiler()
  private val javaCompiler =
      ToolProvider.getSystemJavaCompiler() ?: Log.shared.fatal("No Java compiler found.")

  fun compileKotlin(file: File): ByteArray {
    // Capture logs using byte stream.
    val byteStream = ByteArrayOutputStream()
    val printStream = PrintStream(byteStream)

    // Set input and output directories.
    val args =
        arrayOf(
            file.absolutePath,
            "-d",
            "/tmp/jvm-runner/",
            "-cp",
            System.getProperty("java.class.path"),
            "-no-stdlib")

    // Execute compiler.
    val exitCode = kotlinCompiler.exec(printStream, *args)
    if (exitCode.code != 0) {
      printStream.flush()
      println(byteStream.toString())
      Log.shared.fatal("ERROR: compilation failed")
    }

    // Read compiled class from disk.
    val result = File("/tmp/jvm-runner/${file.nameWithoutExtension}.class")
    Log.shared.debug("Loading file://${result.absolutePath}")

    return result.readBytes()
  }

  fun compileJava(file: File): ByteArray {
    Log.shared.info("Compiling file://${file.absolutePath}")

    // Prepare compilation.
    val files = listOf(file)
    val fileManager = javaCompiler.getStandardFileManager(null, null, null)
    val results = MemoryFileManager(fileManager)
    val compilationUnits = fileManager.getJavaFileObjectsFromFiles(files)
    val diagnosticCollector = DiagnosticCollector<JavaFileObject>()

    // Create a compilation task.
    val task =
        javaCompiler.getTask(
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
    diagnosticCollector.diagnostics.forEach { Log.shared.info(it.toString()) }

    if (!success) {
      Log.shared.fatal("ERROR: compilation failed")
    }

    return results.get(file.nameWithoutExtension)
  }

  companion object {
    private val instance = Compiler()

    fun compileJava(file: File): ByteArray {
      return instance.compileJava(file)
    }

    fun compileKotlin(file: File): ByteArray {
      return instance.compileKotlin(file)
    }
  }
}
