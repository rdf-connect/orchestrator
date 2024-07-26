package technology.idlab.runner.impl.grpc

import EmptyOuterClass.Empty
import Intermediate as GRPC
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage

internal val empty = Empty.getDefaultInstance()

internal fun IRParameter.Type.toGRPC(): GRPC.IRParameterType {
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

internal fun IRParameter.Presence.toGRPC(): GRPC.IRParameterPresence {
  return when (this) {
    IRParameter.Presence.REQUIRED -> GRPC.IRParameterPresence.REQUIRED
    IRParameter.Presence.OPTIONAL -> GRPC.IRParameterPresence.OPTIONAL
  }
}

internal fun IRParameter.Count.toGRPC(): GRPC.IRParameterCount {
  return when (this) {
    IRParameter.Count.SINGLE -> GRPC.IRParameterCount.SINGLE
    IRParameter.Count.LIST -> GRPC.IRParameterCount.LIST
  }
}

internal fun Map<String, IRParameter>.toGRPC(): GRPC.IRParameters {
  return GRPC.IRParameters.newBuilder().putAllParameters(mapValues { it.value.toGRPC() }).build()
}

internal fun List<String>.toGRPC(): GRPC.IRArgumentSimple {
  return GRPC.IRArgumentSimple.newBuilder().addAllValue(this).build()
}

internal fun List<Map<String, IRArgument>>.toGRPC(): GRPC.IRArgumentComplex {
  val arguments = map { it.toGRPC() }
  val builder = GRPC.IRArgumentComplex.newBuilder()
  builder.addAllValue(arguments)
  return builder.build()
}

internal fun Map<String, IRArgument>.toGRPC(): GRPC.IRArgumentMap {
  val builder = GRPC.IRArgumentMap.newBuilder()
  forEach { (key, value) -> builder.putArguments(key, value.toGRPC()) }
  return builder.build()
}

internal fun IRParameter.toGRPC(): GRPC.IRParameter {
  val builder = GRPC.IRParameter.newBuilder()
  when (kind) {
    IRParameter.Kind.SIMPLE -> builder.setSimple(getSimple().toGRPC())
    IRParameter.Kind.COMPLEX -> builder.setComplex(getComplex().toGRPC())
  }
  builder.setPresence(presence.toGRPC())
  builder.setCount(count.toGRPC())
  return builder.build()
}

internal fun IRArgument.toGRPC(): GRPC.IRArgument {
  val builder = GRPC.IRArgument.newBuilder()
  when (kind) {
    IRArgument.Kind.SIMPLE -> builder.setSimple(getSimple().toGRPC())
    IRArgument.Kind.COMPLEX -> {
      builder.setComplex(getComplex().toGRPC())
    }
  }
  return builder.build()
}

internal fun IRStage.toGRPC(): GRPC.IRStage {
  val builder = GRPC.IRStage.newBuilder()
  builder.setUri(uri)
  builder.setProcessor(this.processor.toGRPC())
  builder.putAllArguments(arguments.mapValues { it.value.toGRPC() })
  return builder.build()
}

internal fun IRProcessor.toGRPC(): GRPC.IRProcessor {
  val builder = GRPC.IRProcessor.newBuilder()
  builder.setUri(uri)
  builder.setEntrypoint(entrypoint)
  builder.putAllParameters(parameters.mapValues { it.value.toGRPC() })
  builder.putAllMetadata(metadata)
  return builder.build()
}
