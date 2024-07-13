package runner.impl

import java.io.File
import kotlin.concurrent.thread
import kotlinx.coroutines.channels.Channel
import technology.idlab.extensions.rawPath
import technology.idlab.runner.impl.GRPCRunner
import technology.idlab.util.Log

class HostedGRPCRunner(command: String, directory: File, fromProcessors: Channel<Payload>) :
    GRPCRunner(fromProcessors, "localhost", 5000) {
  // The spawned child process.
  private val process: Process

  init {
    Log.shared.info("Running command: $command in ${directory.rawPath()}")

    val builder = ProcessBuilder(command.split(" "))
    builder.directory(File(directory.rawPath()))

    // Assign the process to the class variable.
    this.process =
        try {
          builder.start()
        } catch (e: Exception) {
          Log.shared.fatal("Failed to start process: ${e.message}")
        }

    // Add a shutdown hook to ensure that the process is killed when the JVM exits.
    Runtime.getRuntime()
        .addShutdownHook(
            thread(start = false) {
              Log.shared.info("Killing child process.")
              this.process.destroyForcibly().waitFor()
              Log.shared.info("Child process killed.")
            })

    // Listen to incoming messages.
    thread {
      val stream = process.inputStream.bufferedReader()
      for (line in stream.lines()) {
        Log.shared.info(line, location = command)
      }
    }

    // Listen for incoming errors.
    thread {
      val stream = process.errorStream.bufferedReader()
      for (line in stream.lines()) {
        Log.shared.fatal(line, location = command)
      }
    }
  }

  override suspend fun exit() {
    Log.shared.debug("Exiting NodeRunner.")
    super.exit()

    Log.shared.debug("Killing child process.")
    process.destroy()
  }
}
