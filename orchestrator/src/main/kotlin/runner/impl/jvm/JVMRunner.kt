package technology.idlab.runner.impl.jvm

import java.net.MalformedURLException
import java.net.URL
import java.net.URLClassLoader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import technology.idlab.InvalidJarPathException
import technology.idlab.InvalidProcessorException
import technology.idlab.MissingMetadataException
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRStage
import technology.idlab.intermediate.argument.LiteralArgument
import technology.idlab.intermediate.argument.NestedArgument
import technology.idlab.intermediate.parameter.LiteralParameterType
import technology.idlab.runner.Runner
import technology.idlab.util.Log

private const val JVM_RUNNER_URI = "https://www.rdf-connect/#JVMRunner"

/**
 * Return a class loader. If a path is given, return a URLClassLoader which loads the JAR file at
 * the given path. Otherwise, return the system class loader.
 *
 * @param path The optional path to a JAR file.
 */
private fun getClassLoader(path: String = ""): ClassLoader {
  // Default case, return the system class loader.
  if (path == "") {
    return ClassLoader.getSystemClassLoader()
  }

  // Parse the path as a URL.
  val url =
      try {
        URL(path)
      } catch (e: MalformedURLException) {
        throw InvalidJarPathException(path)
      }

  // Return a new URLClassLoader with the given URL.
  return URLClassLoader(listOf(url).toTypedArray())
}

class JVMRunner(stages: Collection<IRStage>) : Runner(stages) {
  /** The URI of this runner. */
  override val uri = JVM_RUNNER_URI

  /** Map of all stages in the runner. */
  private val instances = mutableMapOf<String, Processor>()

  /** Incoming messages are delegated to sub channels. These are mapped by their URI. */
  private val readers = mutableMapOf<String, Channel<ByteArray>>()

  init {
    for (stage in stages) {
      loadStage(stage)
    }
  }

  /** Execute all stages in the runner by calling their `exec` method in parallel. */
  override suspend fun exec() = coroutineScope {
    this@JVMRunner.instances.values.map { launch { it.exec() } }.forEach { it.join() }
  }

  /** Closes all readers and exits the runner. */
  override suspend fun exit() {
    for (reader in this.readers.values) {
      reader.close()
    }
  }

  /**
   * Propagate a message to the correct reader channel.
   *
   * @param uri The URI of the reader to send the message to.
   */
  override fun receiveBrokerMessage(uri: String, data: ByteArray) {
    val reader = this.readers[uri]

    if (reader == null) {
      Log.shared.debug { "Channel not found: '$uri'" }
    }

    scheduleTask { reader?.send(data) }
  }

  /**
   * Close a reader by removing it from the `readers` map and close the channel.
   *
   * @param uri The URI of the reader to close.
   */
  override fun closingBrokerChannel(uri: String) {
    val reader = this.readers[uri]

    if (reader == null) {
      Log.shared.debug { "Channel not found: '$uri'" }
    }

    reader?.close()
  }

  private fun IRArgument.unmarshall(): Map<String, List<Any>> {
    val result = mutableMapOf<String, List<Any>>()

    for ((name, argument) in this.root) {
      result[name] =
          when (argument) {
            is LiteralArgument -> argument.unmarshall()
            is NestedArgument -> argument.unmarshall()
          }
    }

    return result
  }

  private fun NestedArgument.unmarshall(): List<Map<String, Any>> {
    val result = mutableListOf<Map<String, Any>>()

    for (value in this.values) {
      val innerResult = mutableMapOf<String, Any>()

      for ((name, argument) in value) {
        innerResult[name] =
            when (argument) {
              is LiteralArgument -> argument.unmarshall()
              is NestedArgument -> argument.unmarshall()
            }
      }

      result.add(innerResult)
    }

    return result
  }

  private fun LiteralArgument.unmarshall(): List<Any> {
    val result = mutableListOf<Any>()

    for (value in values) {
      val unmarshalled = parameter.type.unmarshall(value)
      result.add(unmarshalled)
    }

    return result
  }

  /**
   * Parse a string to a concrete object using a parameter type.
   *
   * @param type The type of the parameter.
   * @param value The string value to parse.
   */
  private fun LiteralParameterType.unmarshall(value: String): Any {
    return when (this) {
      LiteralParameterType.BOOLEAN -> value.toBoolean()
      LiteralParameterType.BYTE -> value.toByte()
      LiteralParameterType.DATE -> TODO()
      LiteralParameterType.DOUBLE -> value.toDouble()
      LiteralParameterType.FLOAT -> value.toFloat()
      LiteralParameterType.INT -> value.toInt()
      LiteralParameterType.LONG -> value.toLong()
      LiteralParameterType.STRING -> value
      LiteralParameterType.WRITER -> createWriter(value)
      LiteralParameterType.READER -> readers.getOrPut(value) { Channel() }
    }
  }

  /** Create a new channel which */
  private fun createWriter(uri: String): SendChannel<ByteArray> {
    val channel = Channel<ByteArray>()

    scope.launch {
      // Pipe data into the broker.
      for (data in channel) {
        broker.send(uri, data)
      }

      broker.unregister(uri)
    }

    return channel
  }

  /**
   * Load a stage into the JVM.
   *
   * @param stage The stage to load.
   * @throws InvalidProcessorException If the processor is not instantiatable.
   * @throws MissingMetadataException If the class key is missing.
   */
  private fun loadStage(stage: IRStage) {
    /* Load the class into the JVM. */
    val loader = getClassLoader(stage.processor.entrypoint)
    val name =
        stage.processor.metadata["class"] ?: throw MissingMetadataException(stage.uri, "class")
    val clazz = Class.forName(name, true, loader) as Class<*>

    /* Check if instantiatable. */
    if (!Processor::class.java.isAssignableFrom(clazz)) {
      throw InvalidProcessorException(stage.processor.uri)
    }

    /* Build the argument map. */
    val arguments = stage.arguments.unmarshall()

    /* Initialize the stage with the new map. */
    val constructor = clazz.getConstructor(Arguments::class.java)
    val args = Arguments.from(arguments)
    this.instances[stage.uri] = constructor.newInstance(args) as Processor
  }
}
