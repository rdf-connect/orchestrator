package runner.jvm

import kotlin.concurrent.thread
import kotlinx.coroutines.channels.Channel
import runner.Runner
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

class JVMRunner : Runner() {
  private val processors = mutableMapOf<String, Pair<IRProcessor, Class<Processor>>>()
  private val stages = mutableMapOf<String, Processor>()

  override suspend fun prepare(processor: IRProcessor) {
    val className = processor.metadata["class"] ?: Log.shared.fatal("Processor has no class key.")
    val clazz = Class.forName(className) as Class<*>

    if (!Processor::class.java.isAssignableFrom(clazz)) {
      Log.shared.fatal("Processor class does not extend Processor.")
    }

    this.processors[processor.uri] = Pair(processor, clazz as Class<Processor>)
  }

  override suspend fun prepare(stage: IRStage) {
    val processor =
        processors[stage.processor.uri]
            ?: Log.shared.fatal("Unknown processor: ${stage.processor.uri}")
    val irArguments = stage.arguments.associateBy { it.name }

    val arguments = mutableMapOf<String, Any>()
    for (parameter in processor.first.parameters) {
      val irArgument = irArguments[parameter.name]

      if (irArgument == null) {
        if (parameter.presence == IRParameter.Presence.REQUIRED) {
          Log.shared.fatal("Missing required argument: ${parameter.name}")
        }

        continue
      }

      if (parameter.count == IRParameter.Count.SINGLE) {
        if (irArgument.value.size != 1) {
          Log.shared.fatal("Expected single value for argument: ${parameter.name}")
        }

        val serialized = irArgument.value[0]
        arguments[parameter.name] = instantiate(parameter.type, serialized)
        continue
      }

      arguments[parameter.name] = irArgument.value.map { instantiate(parameter.type, it) }
    }

    val constructor = processor.second.getConstructor(Map::class.java)
    this.stages[stage.uri] = constructor.newInstance(arguments) as Processor
  }

  override suspend fun exec() {
    this.stages.values.map { thread { it.exec() } }.map { it.join() }
  }

  override suspend fun status(): Status {
    TODO("Not yet implemented")
  }

  private fun instantiate(type: IRParameter.Type, value: String): Any {
    return when (type) {
      IRParameter.Type.BOOLEAN -> value.toBoolean()
      IRParameter.Type.BYTE -> value.toByte()
      IRParameter.Type.DATE -> TODO()
      IRParameter.Type.DOUBLE -> value.toDouble()
      IRParameter.Type.FLOAT -> value.toFloat()
      IRParameter.Type.INT -> value.toInt()
      IRParameter.Type.LONG -> value.toLong()
      IRParameter.Type.STRING -> value
      IRParameter.Type.WRITER -> return Writer(this.outgoing, value)
      IRParameter.Type.READER -> {
        val channel = Channel<ByteArray>()
        this.readers[value] = channel
        return Reader(channel)
      }
    }
  }
}
