package technology.idlab.runner.impl

import EmptyOuterClass.Empty
import Index.ChannelData as GRPCChannelData
import Intermediate as GRPC
import RunnerGrpcKt
import com.google.protobuf.ByteString
import io.grpc.ManagedChannelBuilder
import io.ktor.utils.io.errors.*
import kotlin.concurrent.thread
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import runner.Runner
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

private val empty = Empty.getDefaultInstance()

private fun IRParameter.Type.toGRPC(): GRPC.IRParameterType {
  return when (this) {
    IRParameter.Type.BOOLEAN -> GRPC.IRParameterType.BOOLEAN
    IRParameter.Type.BYTE -> GRPC.IRParameterType.BYTE
    IRParameter.Type.DATE -> GRPC.IRParameterType.DATE
    IRParameter.Type.DOUBLE -> GRPC.IRParameterType.DOUBLE
    IRParameter.Type.FLOAT -> GRPC.IRParameterType.FLOAT
    IRParameter.Type.INT -> GRPC.IRParameterType.INT
    IRParameter.Type.LONG -> GRPC.IRParameterType.LONG
    IRParameter.Type.STRING -> GRPC.IRParameterType.STRING
    IRParameter.Type.WRITER -> GRPC.IRParameterType.WRITER
    IRParameter.Type.READER -> GRPC.IRParameterType.READER
  }
}

private fun IRParameter.Presence.toGRPC(): GRPC.IRParameterPresence {
  return when (this) {
    IRParameter.Presence.REQUIRED -> GRPC.IRParameterPresence.REQUIRED
    IRParameter.Presence.OPTIONAL -> GRPC.IRParameterPresence.OPTIONAL
  }
}

private fun IRParameter.Count.toGRPC(): GRPC.IRParameterCount {
  return when (this) {
    IRParameter.Count.SINGLE -> GRPC.IRParameterCount.SINGLE
    IRParameter.Count.LIST -> GRPC.IRParameterCount.LIST
  }
}

private fun IRParameter.toGRPC(): GRPC.IRParameter {
  val builder = GRPC.IRParameter.newBuilder()
  builder.setName(name)
  builder.setType(type.toGRPC())
  builder.setPresence(presence.toGRPC())
  builder.setCount(count.toGRPC())
  return builder.build()
}

private fun IRArgument.toGRPC(): GRPC.IRArgument {
  val builder = GRPC.IRArgument.newBuilder()
  builder.setName(name)
  builder.addAllValue(value)
  return builder.build()
}

private fun IRStage.toGRPC(): GRPC.IRStage {
  val builder = GRPC.IRStage.newBuilder()
  builder.setUri(uri)
  builder.setProcessorUri(processor.uri)
  builder.addAllArguments(arguments.map { it.toGRPC() })
  return builder.build()
}

private fun IRProcessor.toGRPC(): GRPC.IRProcessor {
  val builder = GRPC.IRProcessor.newBuilder()
  builder.setUri(uri)
  builder.addAllParameters(parameters.map { it.toGRPC() })
  builder.putAllMetadata(metadata)
  return builder.build()
}

/**
 * This runner has GRPC built-in, so the only configuration that an extending class needs to provide
 * is the host and port of the GRPC server, as well as actually booting the process.
 */
abstract class GRPCRunner(
    outgoing: Channel<Payload> = Channel(),
    host: String,
    protected val port: Int
) : Runner(outgoing) {
  /** Handle to the child process. */
  private val process by lazy { createProcess() }

  /** Create a single stub for all communication. */
  private val grpc: RunnerGrpcKt.RunnerCoroutineStub
  private val parseIncoming: Flow<GRPCChannelData>
  private val parseOutgoing: Flow<GRPCChannelData>

  init {
    // Add a shutdown hook to ensure that the process is killed when the JVM exits.
    Runtime.getRuntime().addShutdownHook(Thread { process.destroyForcibly() })

    // Get the command that was used to start the process.
    val command =
        this.process.info().command().orElseThrow { Log.shared.fatal("Failed to start process.") }

    // Pipe all process output to the logger.
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

    // Initialize the GRPC stub.
    val connection = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    grpc = RunnerGrpcKt.RunnerCoroutineStub(connection)

    // Create a flow for incoming messages.
    parseIncoming =
        flow<GRPCChannelData> {
          for (message in this@GRPCRunner.incoming) {
            Log.shared.info("Sending message to runner with URI: `${message.destinationURI}`")
            val builder = GRPCChannelData.newBuilder()
            builder.setDestinationUri(message.destinationURI)
            builder.setData(ByteString.copyFrom(message.data))
            emit(builder.build())
          }
        }

    // Emit outgoing messages.
    parseOutgoing = grpc.channel(parseIncoming)

    thread {
      runBlocking {
        parseOutgoing.collect {
          val message = Payload(it.destinationUri, it.data.toByteArray())
          Log.shared.info("Received message from runner with URI: `${message.destinationURI}`")
          this@GRPCRunner.outgoing.send(message)
        }
      }
    }
  }

  abstract fun createProcess(): Process

  override suspend fun prepare(processor: IRProcessor) {
    Log.shared.info("Preparing processor: `${processor.uri}`")
    grpc.prepareProcessor(processor.toGRPC())
    Log.shared.info("Done preparing processor: `${processor.uri}`")
  }

  override suspend fun prepare(stage: IRStage) {
    Log.shared.info("Preparing stage: `${stage.uri}`")
    grpc.prepareStage(stage.toGRPC())
    Log.shared.info("Done preparing stage: `${stage.uri}`")
  }

  override suspend fun exec() {
    Log.shared.debug("gRPC::exec::invoke")
    grpc.exec(empty)
    Log.shared.debug("gRPC::exec::success")
  }

  override fun halt() {
    process.destroy()
  }

  override suspend fun status(): Status {
    TODO("Not yet implemented.")
  }
}
