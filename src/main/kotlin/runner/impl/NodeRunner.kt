package runner.impl

import java.io.File
import technology.idlab.runner.impl.GRPCRunner

class NodeRunner : GRPCRunner("localhost", 50051) {
  override val process: Process

  init {
    // Configuration.
    val directory = "/Users/jens/Developer/technology.idlab.jvm-runner/lib/nodejs/build/runtime"
    val command = listOf("node", "index.js")

    // Initialize the process.
    val processBuilder = ProcessBuilder(command)
    processBuilder.directory(File(directory))
    process = processBuilder.start()
  }
}
