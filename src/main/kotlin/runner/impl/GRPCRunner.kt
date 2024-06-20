package technology.idlab.runner.impl

import EmptyOuterClass.Empty
import Intermediate as GRPC
import RunnerGrpcKt
import io.grpc.ManagedChannelBuilder
import runner.Runner
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

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
abstract class GRPCRunner(host: String, port: Int) : Runner() {
  /** Handle to the child process. */
  abstract val process: Process

  /** Create a single stub for all communication. */
  private val grpc: RunnerGrpcKt.RunnerCoroutineStub

  init {
    val builder = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    grpc = RunnerGrpcKt.RunnerCoroutineStub(builder)
  }

  override suspend fun prepare(processor: IRProcessor) {
    grpc.prepareProcessor(processor.toGRPC())
  }

  override suspend fun prepare(stage: IRStage) {
    grpc.prepareStage(stage.toGRPC())
  }

  override suspend fun exec() {
    grpc.exec(empty)
  }

  override fun halt() {
    super.halt()
    process.destroy()
  }

  override suspend fun status(): Status {
    TODO("Not yet implemented.")
  }
}
