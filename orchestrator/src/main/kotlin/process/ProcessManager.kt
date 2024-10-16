package technology.idlab.process

import kotlin.concurrent.thread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.log.Log

/**
 * A wrapper class for processes which automatically logs the output of the process, and which
 * throws an error when the process itself logs to the error stream.
 *
 * @param process The process to manage.
 * @property job The job which manages the coroutine scope.
 * @property coroutineScope The coroutine scope which manages IO operations for the process.
 * @property hooks The exit hooks that must be run when the process exits.
 */
class ProcessManager(
    val process: Process,
) {
  private val job = Job()
  private val coroutineScope = CoroutineScope(job + Dispatchers.Default)
  private val hooks = mutableListOf<suspend (Int) -> Unit>()

  init {
    val executable = process.info().command().get().substringAfterLast("/")
    val arguments = process.info().arguments().get()
    val location = "$executable ${arguments.joinToString(" ")}"

    // Listen for stdout.
    coroutineScope.launch {
      val stream = process.inputStream.bufferedReader()
      for (line in stream.lines()) {
        Log.shared.command(location, line)
      }
    }

    // Listen for stderr.
    coroutineScope.launch {
      val stream = process.errorStream.bufferedReader()
      for (line in stream.lines()) {
        Log.shared.command(location, line)
      }
    }

    // Listen for process exit.
    coroutineScope.launch {
      val code = process.waitFor()

      // Run all hooks and wait the result.
      runBlocking { hooks.forEach { hook -> launch { hook(code) }.join() } }
    }

    // Add a shutdown hook to the JVM to ensure that the process is killed when the JVM exits.
    Runtime.getRuntime().addShutdownHook(thread(start = false) { process.destroy() })
  }

  /**
   * Create a new process manager from a process builder.
   *
   * @param process The process builder to manage.
   * @return A new process manager.
   */
  constructor(process: ProcessBuilder) : this(process.start()) {
    Log.shared.debug { "Started process: ${process.command().joinToString(" ")}" }
  }

  /** Add an exit hook which takes in the return value as a parameter. */
  fun onExit(function: suspend (Int) -> Unit) {
    hooks.add(function)
  }
}
