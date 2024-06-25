package runner.impl

import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.channels.Channel
import technology.idlab.runner.impl.GRPCRunner
import technology.idlab.util.Log

class NodeRunner(fromProcessors: Channel<Payload>, port: Int) :
    GRPCRunner(fromProcessors, "localhost", port) {
  /** Handle to the child process. */
  private val process = createProcess()

  init {
    // Add a shutdown hook to ensure that the process is killed when the JVM exits.
    val killProcess =
        thread(start = false) {
          Log.shared.info("Killing child process.")
          this.process.destroyForcibly().waitFor()
          Log.shared.info("Child process killed.")
        }
    Runtime.getRuntime().addShutdownHook(killProcess)

    // Get the command that was used to start the process.
    val command =
        process.info().command().orElseThrow { Log.shared.fatal("Failed to start process.") }

    // Start the process.
    thread {
      val stream = process.inputStream.bufferedReader()
      for (line in stream.lines()) {
        Log.shared.runtime(command, line)
      }
    }

    thread {
      val stream = process.errorStream.bufferedReader()
      for (line in stream.lines()) {
        Log.shared.runtimeFatal(command, line)
      }
    }
  }

  override suspend fun exit() {
    Log.shared.debug("Exiting NodeRunner.")
    super.exit()

    Log.shared.debug("Killing child process.")
    process.destroy()
  }

  private fun createProcess(): Process {
    // Configuration.
    val relative = "runners/nodejs/build/runtime"
    val directory = File(".").resolve(relative)
    Log.shared.debug("Node working directory: ${directory.canonicalPath}")

    val command = listOf("node", "index.js", "localhost", port.toString())
    Log.shared.info("Starting process: `${command.joinToString(" ")}`")

    // Initialize the process.
    val processBuilder = ProcessBuilder(command)
    processBuilder.directory(directory)
    try {
      return processBuilder.start()
    } catch (e: Exception) {
      Log.shared.fatal("Failed to start process.")
    }
  }
}
