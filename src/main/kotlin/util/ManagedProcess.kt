package technology.idlab.util

import io.ktor.utils.io.errors.*
import kotlin.concurrent.thread
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.extensions.rawPath

/**
 * A wrapper class for processes which automatically logs the output of the process, and which
 * throws an error when the process itself logs to the error stream.
 *
 * @param process The process to manage.
 * @param name The name of the process, used for logging purposes.
 */
class ManagedProcess(private val process: Process, private val name: String) {
  // Functions to execute after the process exits, either forced or naturally.
  private val hooks = mutableListOf<suspend (Int) -> Unit>()

  private val exitHook = thread {
    // Retrieve the exit code of the process. If the thread is cancelled before the process exits,
    // we will destroy the process manually.
    val code =
        try {
          process.waitFor()
        } catch (e: IllegalThreadStateException) {
          destroy()
        }

    // Notify the user that the process has exited early.
    Log.shared.info("Process exited with code $code: ${this.name}")

    // Run all hooks and wait the result.
    runBlocking { hooks.forEach { hook -> launch { hook(code) }.join() } }
  }

  // The console input stream of the process.
  private val incoming = thread {
    val stream = process.inputStream.bufferedReader()
    for (line in stream.lines()) {
      Log.shared.command(line, location = name, pid = process.pid())
    }
  }

  // The console error stream of the process.
  private val outgoing = thread {
    val stream = process.errorStream.bufferedReader()
    for (line in stream.lines()) {
      Log.shared.severe(line, location = name)
    }
  }

  init {
    // Add a shutdown hook to the JVM to ensure that the process is killed when the JVM exits.
    Runtime.getRuntime().addShutdownHook(thread(start = false) { this@ManagedProcess.destroy() })
  }

  /** Add an exit hook which takes in the return value as a parameter. */
  fun exitHook(function: suspend (Int) -> Unit) {
    this.hooks.add(function)
  }

  /** Retrieve the process ID of the managed process as a string. */
  private fun pid(): String {
    return try {
      process.pid().toString()
    } catch (e: UnsupportedOperationException) {
      "Unknown"
    }
  }

  /** Wait for the process to finish. */
  fun waitFor(): Int {
    return process.waitFor()
  }

  /** Attempt to destroy the process immediately. Note that this will not run the exit hooks! */
  private fun destroy(): Int {
    // Destroy the process and wait for it to exit, return the code.
    val exitCode =
        try {
          Log.shared.debug { "Killing process: ${this.pid()}" }
          process.destroyForcibly().waitFor()
        } catch (e: InterruptedException) {
          Log.shared.fatal("Failed to kill process: ${e.message}")
        }

    // Interrupt the input and output streams.
    this.incoming.interrupt()
    this.outgoing.interrupt()

    return exitCode
  }

  companion object {
    fun from(builder: ProcessBuilder, attempts: Int = 5): ManagedProcess {
      Log.shared.debug {
        val command = builder.command().joinToString(" ")
        val directory = builder.directory().rawPath()
        "Executing command: $command with working directory: $directory."
      }

      // Create the process.
      val process = runBlocking {
        retries(attempts) {
          try {
            builder.start()
          } catch (e: IOException) {
            Log.shared.fatal("Failed to start process: ${e.message}")
          }
        }
      }

      return ManagedProcess(process, builder.command().joinToString(" "))
    }
  }
}
