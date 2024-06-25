package technology.idlab.runner.impl

import EmptyOuterClass.Empty
import Index.ChannelData as GRPCChannelData
import Intermediate as GRPC
import RunnerGrpcKt
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
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
abstract class GRPCRunner(fromProcessors: Channel<Payload>, host: String, protected val port: Int) :
    Runner(fromProcessors) {

  /** Create a single stub for all communication. */
  // Initialize the GRPC stub.
  private val conn: ManagedChannel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
  private val grpc: RunnerGrpcKt.RunnerCoroutineStub = RunnerGrpcKt.RunnerCoroutineStub(conn)

  override suspend fun exit() {
    Log.shared.debug("Exiting GRPCRunner.")
    super.exit()

    Log.shared.debug("Shutting down connection.")
    conn.shutdown()
  }

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

  override suspend fun exec() = coroutineScope {
    Log.shared.debug("gRPC::exec::invoke")
    grpc.exec(empty)
    Log.shared.debug("gRPC::exec::success")

    // Create a flow for outgoing messages.
    val toGRPCProcessors =
        toProcessors.receiveAsFlow().map {
          Log.shared.debug("'${it.data.decodeToString()}' -> [${it.channel}]")
          val builder = GRPCChannelData.newBuilder()
          builder.setDestinationUri(it.channel)
          builder.setData(ByteString.copyFrom(it.data))
          builder.build()
        }

    // Create a flow for incoming messages.
    Log.shared.debug("Begin routing messages in GRPCRunner.")
    grpc
        .channel(toGRPCProcessors)
        .map { Payload(it.destinationUri, it.data.toByteArray()) }
        .collect {
          Log.shared.debug("'${it.data.decodeToString()}' -> [${it.channel}]")
          fromProcessors.send(it)
        }
    Log.shared.debug("Ending routing messages in GRPCRunner.")
  }
}
