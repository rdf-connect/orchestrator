package runner.impl

import java.io.File
import technology.idlab.runner.impl.GRPCRunner
import technology.idlab.util.Log

class NodeRunner(port: Int) : GRPCRunner("localhost", port) {
  override fun createProcess(): Process {
    // Configuration.
    val directory = "/Users/jens/Developer/technology.idlab.jvm-runner/runners/nodejs/build/runtime"
    val command = listOf("node", "index.js", "localhost", port.toString())
    Log.shared.info("Starting process: `${command.joinToString(" ")}`")

    // Initialize the process.
    val processBuilder = ProcessBuilder(command)
    processBuilder.directory(File(directory))
    try {
      return processBuilder.start()
    } catch (e: Exception) {
      Log.shared.fatal("Failed to start process.")
    }
  }
}
