package technology.idlab.rdfc.orchestrator.runner.grpc

import com.google.protobuf.Timestamp
import com.google.protobuf.kotlin.toByteStringUtf8
import rdfc.Intermediate as GRPC
import rdfc.Intermediate
import technology.idlab.rdfc.intermediate.IRProcessor
import technology.idlab.rdfc.intermediate.IRStage
import technology.idlab.rdfc.intermediate.argument.Argument
import technology.idlab.rdfc.intermediate.argument.LiteralArgument
import technology.idlab.rdfc.intermediate.argument.NestedArgument
import technology.idlab.rdfc.intermediate.parameter.LiteralParameterType

private fun serialize(arg: LiteralArgument, serialized: String): Intermediate.ArgumentLiteral =
    rdfc.argumentLiteral {
      when (arg.parameter.type) {
        LiteralParameterType.BOOLEAN -> {
          bool = serialized == "true"
        }
        LiteralParameterType.BYTE -> {
          bytes = serialized.toByteStringUtf8()
        }
        LiteralParameterType.DATE -> {
          timestamp = Timestamp.parseFrom(serialized.toByteArray())
        }
        LiteralParameterType.DOUBLE -> {
          double = serialized.toDouble()
        }
        LiteralParameterType.FLOAT -> {
          float = serialized.toFloat()
        }
        LiteralParameterType.INT -> {
          int32 = serialized.toInt()
        }
        LiteralParameterType.LONG -> {
          int64 = serialized.toLong()
        }
        LiteralParameterType.STRING -> {
          string = serialized
        }
        LiteralParameterType.WRITER -> {
          writer = rdfc.writer { uri = serialized }
        }
        LiteralParameterType.READER -> {
          reader = rdfc.reader { uri = serialized }
        }
      }
    }

private fun serialize(arg: Argument): Intermediate.Argument {
  return when (arg) {
    is LiteralArgument -> {
      if (arg.parameter.single) {
        rdfc.argument { literal = serialize(arg, arg.values.single()) }
      } else {
        rdfc.argument {
          this.literals =
              rdfc.ArgumentLiteralKt.list { values.addAll(arg.values.map { serialize(arg, it) }) }
        }
      }
    }
    is NestedArgument -> {
      rdfc.argument {
        if (arg.parameter.single) {
          this.map =
              rdfc.argumentMap {
                values.putAll(arg.values.single().mapValues { serialize(it.value) })
              }
        } else {
          this.maps =
              rdfc.ArgumentMapKt.list {
                for (nestedArgument in arg.values) {
                  values.add(
                      rdfc.argumentMap {
                        values.putAll(nestedArgument.mapValues { serialize(it.value) })
                      })
                }
              }
        }
      }
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

    for ((key, value) in stage.arguments.root) {
      arguments[key] = serialize(value)
    }
  }
}
