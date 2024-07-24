package technology.idlab.runner.impl.jvm

import arrow.core.zip
import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import technology.idlab.broker.Broker
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.Runner
import technology.idlab.util.Log

private const val STAGE_NO_CLASS = "Processor has no class key set."
private const val REQUIRES_PROCESSOR_BASE_CLASS = "Class does not extend Processor."

/**
 * Return a class loader. If a path is given, return a URLClassLoader which loads the JAR file at
 * the given path. Otherwise, return the system class loader.
 *
 * @param path The optional path to a JAR file.
 */
private fun getClassLoader(path: String? = null): ClassLoader {
  // Default case, return the system class loader.
  if (path == null) {
    return ClassLoader.getSystemClassLoader()
  }

  // Parse the path as a URL.
  val url =
      try {
        URL(path)
      } catch (e: MalformedURLException) {
        Log.shared.fatal("Invalid entrypoint '$path' for processor.")
      }

  // Return a new URLClassLoader with the given URL.
  return URLClassLoader(listOf(url).toTypedArray())
}

class JVMRunner(broker: Broker<ByteArray>) : Runner(broker) {
  /** The URI of this runner. */
  override val uri = "https://www.rdf-connect.com/#JVMRunner"

  /** Map of all stages in the runner. */
  private val stages = mutableMapOf<String, Processor>()

  /** Incoming messages are delegated to sub channels. These are mapped by their URI. */
  private val readers = mutableMapOf<String, Channel<ByteArray>>()

  private val job = Job()
  private val scope = CoroutineScope(job)

  /** Load a new processor into the runner. */
  override suspend fun load(processor: IRProcessor, stage: IRStage) {
    /** Load the class into the JVM. */
    val loader = getClassLoader(processor.entrypoint)
    val name = processor.metadata["class"] ?: Log.shared.fatal(STAGE_NO_CLASS)
    val clazz = Class.forName(name, true, loader) as Class<*>

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

  /** Execute all stages in the runner by calling their `exec` method in parallel. */
  override suspend fun exec() = coroutineScope {
    this@JVMRunner.stages.values.map { launch { it.exec() } }.forEach { it.join() }
  }

  /** Closes all readers and exits the runner. */
  override suspend fun exit() {
    Log.shared.info("Exiting the JVM Runner.")

    // Close all readers.
    for (reader in this.readers.values) {
      reader.close()
    }
  }

  /**
   * Propagate a message to the correct reader channel.
   *
   * @param uri The URI of the reader to send the message to.
   */
  override suspend fun receive(uri: String, data: ByteArray) {
    val reader = this.readers[uri]

    if (reader == null) {
      Log.shared.debug { "Channel not found: '$uri'" }
    }

    reader?.send(data)
  }

  /**
   * Close a reader by removing it from the `readers` map and close the channel.
   *
   * @param uri The URI of the reader to close.
   */
  override suspend fun close(uri: String) {
    val reader = this.readers[uri]

    if (reader == null) {
      Log.shared.debug { "Channel not found: '$uri'" }
    }

    reader?.close()
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

  /**
   * Parse a string to a concrete object using a parameter type.
   *
   * @param type The type of the parameter.
   * @param value The string value to parse.
   */
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
      IRParameter.Type.WRITER -> createWriter(value)
      IRParameter.Type.READER -> this.readers.getOrPut(value) { Channel() }
    }
  }

  /** Create a new channel which */
  private fun createWriter(uri: String): SendChannel<ByteArray> {
    val channel = Channel<ByteArray>()

    // Pipe data into the broker.
    scope.launch {
      for (data in channel) {
        broker.send(uri, data)
      }

      broker.unregister(uri, this@JVMRunner)
    }

    return channel
  }
}
