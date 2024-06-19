package technology.idlab.runtime.impl

import RunnerGrpcKt
import com.google.protobuf.kotlin.toByteString
import io.grpc.ManagedChannelBuilder
import java.io.BufferedReader
import java.io.File
import kotlinx.coroutines.flow.flow
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.runtime.Runner

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
    val builder = RunnerOuterClass.Processor.newBuilder().setUri(processor.uri)

    // TODO: Implement the rest of the types.
    grpc.prepareProcessor(builder.build())
  }

  override suspend fun prepare(stage: IRStage) {
    // Init builder and set the stage URI and processor URI.
    val builder =
        RunnerOuterClass.Stage.newBuilder().setUri(stage.uri).setProcessorUri(stage.processor.uri)

    // Insert all processor arguments.
    stage.arguments.forEach { (name, value) ->
      val arg =
          RunnerOuterClass.Argument.newBuilder()
              .setType(RunnerOuterClass.ArgumentType.STRING)
              //          .setValue(value.second.toByteStringUtf8())
              .build()
      builder.putArguments(name, arg)
    }

    // Execute the call.
    grpc.prepareStage(builder.build())
  }

  override suspend fun exec() {
    // On incoming data, send it to the channel.
    val requests =
        flow<RunnerOuterClass.Payload> {
          for (payload in outgoing) {
            val req = RunnerOuterClass.Payload.newBuilder()
            req.setChannelUri(payload.first)
            req.setData(payload.second.toByteString())
            emit(req.build())
          }
        }

    // Initialize the stream.
    val responses = grpc.channel(requests)

    // Capture the responses down the channel.
    responses.collect { incoming.send(it.channelUri to it.data.toByteArray()) }

    // Execute the call.
    grpc.exec(RunnerOuterClass.Void.getDefaultInstance())
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
