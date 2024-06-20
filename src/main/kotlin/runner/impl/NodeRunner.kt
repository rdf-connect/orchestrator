package runner.impl

import RunnerGrpcKt
import io.grpc.ManagedChannelBuilder
import java.io.BufferedReader
import java.io.File
import runner.Runner
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

class NodeRunner : Runner() {
  // Process runtime.
  private val process: Process
  private val input: BufferedReader
  private val error: BufferedReader

  // gRPC Channels
  private val grpc =
      ManagedChannelBuilder.forAddress("localhost", 50051).usePlaintext().build().let {
        RunnerGrpcKt.RunnerCoroutineStub(it)
      }

  init {
    // Configuration.
    val directory = "/Users/jens/Developer/technology.idlab.jvm-runner/lib/nodejs/build/runtime"
    val command = listOf("node", "index.js")

    // Initialize the process.
    val processBuilder = ProcessBuilder(command)
    processBuilder.directory(File(directory))
    process = processBuilder.start()

    // Link input and output streams.
    input = process.inputStream.bufferedReader()
    error = process.errorStream.bufferedReader()
  }

  override suspend fun prepare(processor: IRProcessor) {
    TODO("Not yet implemented")
  }

  override suspend fun prepare(stage: IRStage) {
    TODO("Not yet implemented")
  }

  override suspend fun exec() {
    TODO("Not yet implemented")
  }

  override suspend fun status(): Status {
    // If the process has finished.
    if (!process.isAlive) {
      return if (process.exitValue() == 0) {
        Status.FINISHED
      } else {
        Status.ERROR
      }
    }

    TODO("Not yet implemented")
  }
}
