package technology.idlab.runner.impl

import EmptyOuterClass.Empty
import Index.ChannelData as GRPCChannelData
import Intermediate as GRPC
import RunnerGrpcKt
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import runner.Runner
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.util.Log
import technology.idlab.util.retries

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

private fun Map<String, IRParameter>.toGRPC(): GRPC.IRParameters {
  return GRPC.IRParameters.newBuilder().putAllParameters(mapValues { it.value.toGRPC() }).build()
}

private fun List<String>.toGRPC(): GRPC.IRArgumentSimple {
  return GRPC.IRArgumentSimple.newBuilder().addAllValue(this).build()
}

private fun List<Map<String, IRArgument>>.toGRPC(): GRPC.IRArgumentComplex {
  val arguments = map { it.toGRPC() }
  val builder = GRPC.IRArgumentComplex.newBuilder()
  builder.addAllValue(arguments)
  return builder.build()
}

private fun Map<String, IRArgument>.toGRPC(): GRPC.IRArgumentMap {
  val builder = GRPC.IRArgumentMap.newBuilder()
  forEach { (key, value) -> builder.putArguments(key, value.toGRPC()) }
  return builder.build()
}

private fun IRParameter.toGRPC(): GRPC.IRParameter {
  val builder = GRPC.IRParameter.newBuilder()
  when (kind) {
    IRParameter.Kind.SIMPLE -> builder.setSimple(getSimple().toGRPC())
    IRParameter.Kind.COMPLEX -> builder.setComplex(getComplex().toGRPC())
  }
  builder.setPresence(presence.toGRPC())
  builder.setCount(count.toGRPC())
  return builder.build()
}

private fun IRArgument.toGRPC(): GRPC.IRArgument {
  val builder = GRPC.IRArgument.newBuilder()
  when (kind) {
    IRArgument.Kind.SIMPLE -> builder.setSimple(getSimple().toGRPC())
    IRArgument.Kind.COMPLEX -> {
      builder.setComplex(getComplex().toGRPC())
    }
  }
  return builder.build()
}

private fun IRStage.toGRPC(processor: GRPC.IRProcessor): GRPC.IRStage {
  val builder = GRPC.IRStage.newBuilder()
  builder.setUri(uri)
  builder.setProcessor(processor)
  builder.putAllArguments(arguments.mapValues { it.value.toGRPC() })
  return builder.build()
}

private fun IRProcessor.toGRPC(): GRPC.IRProcessor {
  val builder = GRPC.IRProcessor.newBuilder()
  builder.setUri(uri)
  builder.setEntrypoint(entrypoint)
  builder.putAllParameters(parameters.mapValues { it.value.toGRPC() })
  builder.putAllMetadata(metadata)
  return builder.build()
}

/**
 * This runner has GRPC built-in, so the only configuration that an extending class needs to provide
 * is the host and port of the GRPC server, as well as actually booting the process.
 */
abstract class GRPCRunner(
    /** The channel to receive messages from the processors. */
    fromProcessors: Channel<Payload>,
    host: String,
    /** The port of the GRPC server. */
    private val port: Int
) : Runner(fromProcessors) {
  /** Create a single stub for all communication. */
  private val conn: ManagedChannel =
      ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
  private val grpc: RunnerGrpcKt.RunnerCoroutineStub = RunnerGrpcKt.RunnerCoroutineStub(conn)

  override suspend fun exit() {
    Log.shared.debug("Exiting GRPCRunner.")
    super.exit()

    Log.shared.debug("Shutting down connection.")
    conn.shutdown()
  }

  override suspend fun load(processor: IRProcessor, stage: IRStage) {
    val payload = stage.toGRPC(processor.toGRPC())
    retries(5, 1000) { grpc.load(payload) }
  }

  override suspend fun exec() = coroutineScope {
    val router = async {
      // Create a flow for outgoing messages.
      val toGRPCProcessors =
          toProcessors.receiveAsFlow().map {
            Log.shared.debug(
                "'${it.data.decodeToString().replace("\n", "\\n")}' -> [${it.channel}]")
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
            Log.shared.debug(
                "'${it.data.decodeToString().replace("\n", "\\n")}' -> [${it.channel}]")
            fromProcessors.send(it)
          }
      Log.shared.debug("Ending routing messages in GRPCRunner.")
    }

    retries(5, 1000) { grpc.exec(empty) }

    router.await()
  }
}
