package technology.idlab.runner

import java.util.Optional
import technology.idlab.logging.Log

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
    val result = arguments[name] as T ?: log.fatal("Argument $name is missing")
    return result
  }

  fun <T> getOptionalArgument(name: String): Optional<T> {
    val result = arguments[name] as T?
    return Optional.ofNullable(result) as Optional<T>
  }

  abstract fun exec()
}
