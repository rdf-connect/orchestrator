package technology.idlab.runner.impl.grpc

import com.google.protobuf.Timestamp
import com.google.protobuf.kotlin.toByteStringUtf8
import rdfc.Intermediate as GRPC
import rdfc.Intermediate
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage

private fun serialize(type: IRParameter.Type, serialized: String): Intermediate.ArgumentLiteral =
    rdfc.argumentLiteral {
      when (type) {
        IRParameter.Type.BOOLEAN -> {
          bool = serialized == "true"
        }
        IRParameter.Type.BYTE -> {
          bytes = serialized.toByteStringUtf8()
        }
        IRParameter.Type.DATE -> {
          timestamp = Timestamp.parseFrom(serialized.toByteArray())
        }
        IRParameter.Type.DOUBLE -> {
          double = serialized.toDouble()
        }
        IRParameter.Type.FLOAT -> {
          float = serialized.toFloat()
        }
        IRParameter.Type.INT -> {
          int32 = serialized.toInt()
        }
        IRParameter.Type.LONG -> {
          int64 = serialized.toLong()
        }
        IRParameter.Type.STRING -> {
          string = serialized
        }
        IRParameter.Type.WRITER -> {
          writer = rdfc.writer { uri = serialized }
        }
        IRParameter.Type.READER -> {
          reader = rdfc.reader { uri = serialized }
        }
      }
    }

private fun serialize(
    type: IRParameter.Type,
    serialized: List<String>
): Intermediate.ArgumentLiteral.List =
    rdfc.ArgumentLiteralKt.list {
      for (element in serialized) {
        values.add(serialize(type, element))
      }
    }

private fun serialize(serialized: Map<String, IRArgument>): Intermediate.ArgumentMap =
    rdfc.argumentMap { values.putAll(serialized.mapValues { serialize(it.value) }) }

private fun serialize(serialized: List<Map<String, IRArgument>>): Intermediate.ArgumentMap.List =
    rdfc.ArgumentMapKt.list { values.addAll(serialized.map { serialize(it) }) }

private fun serialize(arg: IRArgument): Intermediate.Argument =
    rdfc.argument {
      when (Pair(arg.parameter.count, arg.parameter.kind)) {
        Pair(IRParameter.Count.SINGLE, IRParameter.Kind.SIMPLE) -> {
          val type = arg.parameter.getSimple()
          val serialized = arg.getSimple()[0]
          this.literal = serialize(type, serialized)
        }
        Pair(IRParameter.Count.LIST, IRParameter.Kind.SIMPLE) -> {
          val type = arg.parameter.getSimple()
          val serialized = arg.getSimple()
          this.literals = serialize(type, serialized)
        }
        Pair(IRParameter.Count.SINGLE, IRParameter.Kind.COMPLEX) -> {
          this.map = serialize(arg.getComplex()[0])
        }
        Pair(IRParameter.Count.LIST, IRParameter.Kind.COMPLEX) -> {
          this.maps = serialize(arg.getComplex())
        }
      }
    }

private fun serialize(processor: IRProcessor): GRPC.Processor {
  return rdfc.processor {
    uri = processor.uri
    entrypoint = processor.entrypoint
    metadata.putAll(processor.metadata)
  }
}

internal fun serialize(stage: IRStage): GRPC.Stage {
  return rdfc.stage {
    uri = stage.uri
    processor = serialize(stage.processor)
    arguments.putAll(stage.arguments.mapValues { serialize(it.value) })
  }
}
