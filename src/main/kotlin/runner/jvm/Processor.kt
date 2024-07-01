package runner.jvm

import java.util.Optional
import technology.idlab.util.Log

abstract class Processor(
    /**
     * The arguments of a processor are stored in a map and can be accessed by name. At the time of
     * writing, the user must manually cast the arguments to the correct type.
     */
    private val arguments: Map<String, Any>,
) {
  /**
   * Processors which wish to log messages should use the logger provided by the template class.
   * This logger is created with the name of the class which extends the template.
   */
  @JvmField protected val log = Log.shared

  fun <T> getArgument(name: String): T {
    val result = arguments.get(name) as List<T> ?: log.fatal("Argument $name is missing")
    return result.get(0) ?: log.fatal("Argument $name is missing")
  }

  fun <T> getNullableArgument(name: String): T? {
    val result = arguments.get(name) as List<T>?
    return result?.get(0)
  }

  fun <T> getOptionalArgument(name: String): Optional<T> {
    val result = (arguments.get(name) as List<T>?)?.get(0) ?: log.fatal("Argument $name is missing")

    if (result is Optional<*>) {
      return result as Optional<T>
    }

    log.fatal("Argument $name is not optional")
  }

  abstract suspend fun exec()
}
