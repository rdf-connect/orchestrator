package runner.jvm

import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.utils.addToStdlib.ifFalse
import runner.Runner
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

class JVMRunner(
    fromProcessors: Channel<Payload>,
) : Runner(fromProcessors) {
  /** Map of all stages in the runner. */
  private val stages = mutableMapOf<String, Processor>()

  /** All stages are ran in their own job, for cancellation purposes we keep track of them. */
  private val jobs: MutableList<Job> = mutableListOf()

  /** Incoming messages are delegated to sub channels. These are mapped by their URI. */
  private val readers = mutableMapOf<String, Channel<ByteArray>>()

  override suspend fun load(stage: IRStage) {
    /** Load the class into the JVM> */
    val className =
        stage.processor.metadata["class"] ?: Log.shared.fatal("The processor has no class key set.")
    val clazz = Class.forName(className) as Class<*>

    /** Check if instantiatable. */
    if (!Processor::class.java.isAssignableFrom(clazz)) {
      Log.shared.fatal("Processor class does not extend Processor.")
    }

    /** Build the argument map. */
    val arguments = mutableMapOf<String, Any>()

    for ((name, arg) in stage.arguments) {
      /** Create concrete instances. */
      val concrete = arg.value.map { instantiate(arg.parameter.type, it) }

      /**
       * If an array is expected, simply pass the value directly. Otherwise, pass the first
       * variable.
       */
      if (arg.parameter.count == IRParameter.Count.LIST) {
        arguments[name] = concrete
      } else {
        assert(concrete.size == 1)
        assert(arg.parameter.count == IRParameter.Count.SINGLE)
        arguments[name] = concrete[0]
      }
    }

    /** Check if the non-optional arguments were set. */
    stage.processor.parameters
        .filter { it.value.presence == IRParameter.Presence.REQUIRED }
        .all { it.key in arguments.keys }
        .ifFalse { Log.shared.fatal("Required argument not set.") }

    /** Initialize the stage with the new map. */
    val constructor = clazz.getConstructor(Map::class.java)
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
