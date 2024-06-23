package runner.impl

import java.io.File
import kotlinx.coroutines.channels.Channel
import technology.idlab.runner.impl.GRPCRunner
import technology.idlab.util.Log

class NodeRunner(outgoing: Channel<Payload> = Channel(), port: Int) :
    GRPCRunner(outgoing, "localhost", port) {
  override fun createProcess(): Process {
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
