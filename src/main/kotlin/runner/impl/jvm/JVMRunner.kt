package technology.idlab.runner.impl.jvm

import arrow.core.zip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import technology.idlab.extensions.map
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.Runner
import technology.idlab.util.Log

private const val STAGE_NO_CLASS = "Processor has no class key set."
private const val REQUIRES_PROCESSOR_BASE_CLASS = "Class does not extend Processor."

class JVMRunner(
    fromProcessors: Channel<Payload>,
) : Runner(fromProcessors) {
  /** A shared coroutine scope. */
  private val job = Job()
  private val scope = CoroutineScope(Dispatchers.Default + job)

  /** Map of all stages in the runner. */
  private val stages = mutableMapOf<String, Processor>()

  /** Incoming messages are delegated to sub channels. These are mapped by their URI. */
  private val readers = mutableMapOf<String, Channel<ByteArray>>()

  override suspend fun load(processor: IRProcessor, stage: IRStage) {
    /** Load the class into the JVM. */
    val className = processor.metadata["class"] ?: Log.shared.fatal(STAGE_NO_CLASS)
    val clazz = Class.forName(className) as Class<*>

    /** Check if instantiatable. */
    if (!Processor::class.java.isAssignableFrom(clazz)) {
      Log.shared.fatal(REQUIRES_PROCESSOR_BASE_CLASS)
    }

    /** Build the argument map. */
    val arguments = this.instantiate(processor.parameters.zip(stage.arguments))

    /** Initialize the stage with the new map. */
    val constructor = clazz.getConstructor(Arguments::class.java)
    val args = Arguments.from(arguments)
    this.stages[stage.uri] = constructor.newInstance(args) as Processor
  }

  override suspend fun exec() = coroutineScope {
    Log.shared.info("Executing all stages.")

    job.start()

    // Create a new job which routes the messages.
    val router = launch {
      Log.shared.debug("Begin routing messages in JVMRunner.")
      while (isActive) {
        try {
          withTimeout(1000) {
            val message = toProcessors.receive()
            val target = readers[message.channel]!!
            Log.shared.info("'${message.data.decodeToString()}' -> ${message.channel}")
            target.send(message.data)
          }
        } catch (_: TimeoutCancellationException) {}
      }
    }

    // Notify when the router is done.
    router.invokeOnCompletion { Log.shared.debug("Ending routing messages in JVMRunner.") }

    // Initialize a job for all processors.
    this@JVMRunner.stages.values.map { launch { it.exec() } }.forEach { it.join() }

    router.cancel()
  }

  override suspend fun exit() {
    Log.shared.info("Exiting the JVM Runner.")
    super.exit()

    // Close all readers.
    for (reader in this.readers.values) {
      reader.close()
    }
  }

  private fun instantiate(
      serialized: Map<String, Pair<IRParameter, IRArgument>>
  ): Map<String, List<Any>> {
    return serialized.mapValues { (_, map) ->
      val (parameter, arguments) = map

      when (parameter.kind) {
        IRParameter.Kind.SIMPLE -> {
          arguments.getSimple().map { instantiate(parameter.getSimple(), it) }
        }
        IRParameter.Kind.COMPLEX -> {
          arguments.getComplex().map { instantiate(parameter.getComplex().zip(it)) }
        }
      }
    }
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
      IRParameter.Type.WRITER -> {
        this.fromProcessors.map<Payload, ByteArray>(scope) { Payload(value, it) }
      }
      IRParameter.Type.READER -> {
        val channel = this.readers[value] ?: Channel()
        this.readers[value] = channel
        return channel
      }
    }
  }
}
