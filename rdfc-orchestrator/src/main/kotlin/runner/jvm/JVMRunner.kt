package technology.idlab.rdfc.orchestrator.runner.jvm

import java.net.URI
import java.net.URLClassLoader
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import technology.idlab.rdfc.core.log.Log
import technology.idlab.rdfc.intermediate.IRArgument
import technology.idlab.rdfc.intermediate.IRStage
import technology.idlab.rdfc.intermediate.argument.LiteralArgument
import technology.idlab.rdfc.intermediate.argument.NestedArgument
import technology.idlab.rdfc.intermediate.parameter.LiteralParameterType
import technology.idlab.rdfc.orchestrator.broker.exception.UnknownChannelException
import technology.idlab.rdfc.orchestrator.runner.Runner
import technology.idlab.rdfc.processor.Arguments
import technology.idlab.rdfc.processor.Processor

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

  // Return a new URLClassLoader with the given URL.
  val url = URI(path).toURL()
  return URLClassLoader(listOf(url).toTypedArray())
}

class JVMRunner(stages: Collection<IRStage>) : Runner(stages) {
  /** Map of all stages in the runner. */
  private val instances = mutableMapOf<String, Processor>()

  /** Incoming messages are delegated to sub channels. These are mapped by their URI. */
  private val readers = mutableMapOf<String, Channel<ByteArray>>()

  init {
    for (stage in stages) {
      loadStage(stage)
    }
  }

  /* Execute all stages in the runner by calling their `exec` method in parallel. */
  override suspend fun exec() = coroutineScope {
    this@JVMRunner.instances.values.map { launch { it.exec() } }.forEach { it.join() }
  }

  /* Closes all readers and exits the runner. */
  override suspend fun exit() {
    for (reader in readers.values) {
      reader.close()
    }
  }

  /**
   * Propagate a message to the correct reader channel.
   *
   * @param uri The URI of the reader to send the message to.
   * @throws Exception If the reader is not found.
   */
  override fun receiveBrokerMessage(uri: String, data: ByteArray) {
    val reader = readers[uri] ?: throw UnknownChannelException(uri)
    scheduleTask { reader.send(data) }
  }

  /*
   * Close a reader by removing it from the `readers` map and close the channel.
   */
  override fun closingBrokerChannel(uri: String) {
    val reader = this.readers[uri] ?: throw UnknownChannelException(uri)
    scheduleTask { reader.close() }
  }

  /**
   * Unmarshall an argument map to a concrete object, usable by JVM processors.
   *
   * @return A map of argument names to their values.
   */
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

  /**
   * Unmarshall a nested argument to a list of maps.
   *
   * @return A list of maps, where each map represents a nested argument.
   */
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

  /**
   * Unmarshall a literal argument to a list of concrete objects.
   *
   * @return A list of concrete objects.
   */
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

  /**
   * Create a new writer which writes to a given channel.
   *
   * @param uri The URI of the writer.
   * @return A channel representing the writer as a `SendChannel` object.
   */
  private fun createWriter(uri: String): SendChannel<ByteArray> {
    val channel = Channel<ByteArray>()

    scope.launch {
      // Pipe data into the broker.
      for (data in channel) {
        broker.send(uri, data)
      }

      // If all data has been sent, unregister the writer.
      broker.unregister(uri)
    }

    return channel
  }

  /**
   * Load a stage into the JVM.
   *
   * @param stage The stage to load.
   */
  private fun loadStage(stage: IRStage) {
    /* Load the class into the JVM. */
    Log.shared.debug { "Loading stage: ${stage.uri}" }
    val loader = getClassLoader(stage.processor.entrypoint)
    val name = checkNotNull(stage.processor.metadata["class"]) { "Missing class key in processor." }
    Log.shared.debug { "Loading JVM class: $name" }
    val clazz = Class.forName(name, true, loader) as Class<*>

    /* Check if instantiatable. */
    check(Processor::class.java.isAssignableFrom(clazz)) { "Processor is not instantiatable." }

    /* Build the argument map. */
    val arguments = stage.arguments.unmarshall()

    /* Initialize the stage with the new map. */
    val constructor = clazz.getConstructor(Arguments::class.java)
    val args = Arguments.from(arguments)
    this.instances[stage.uri] = constructor.newInstance(args) as Processor
  }
}
