package runner.jvm

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.backend.common.push
import runner.Runner
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

class JVMRunner(
    fromProcessors: Channel<Payload>,
) : Runner(fromProcessors) {
  private val processors = mutableMapOf<String, Pair<IRProcessor, Class<Processor>>>()
  private val stages = mutableMapOf<String, Processor>()
  private val jobs: MutableList<Job> = mutableListOf()

  /** Incoming messages are delegated to sub channels. These are mapped by their URI. */
  private val readers = mutableMapOf<String, Channel<ByteArray>>()

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

  override suspend fun exec() = coroutineScope {
    Log.shared.info("Executing all stages.")

    // Initialize a job for all processors.
    this@JVMRunner.stages.values.forEach {
      val job = launch { it.exec() }
      jobs.push(job)
    }

    // Route all incoming messages.
    Log.shared.debug("Begin routing messages in JVMRunner.")
    while (isActive) {
      withTimeout(1000) {
        val message = toProcessors.receive()
        val target = readers[message.channel]!!
        Log.shared.info("'${message.data.decodeToString()}' -> ${message.channel}")
        target.send(message.data)
      }
    }
    Log.shared.debug("Ending routing messages in JVMRunner.")

    // Await all processors.
    jobs.forEach { it.cancelAndJoin() }
  }

  override suspend fun exit() {
    Log.shared.info("Exiting the JVM Runner.")
    super.exit()

    // Close all readers.
    for (reader in this.readers.values) {
      reader.close()
    }

    // Suspend all jobs.
    jobs.map { it.apply { it.cancel() } }.forEach { it.join() }
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
      IRParameter.Type.WRITER -> return Writer(this.fromProcessors, value)
      IRParameter.Type.READER -> {
        val channel = this.readers[value] ?: Channel()
        this.readers[value] = channel
        return Reader(channel, value)
      }
    }
  }
}
