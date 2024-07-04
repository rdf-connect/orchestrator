package runner.jvm

import technology.idlab.runner.jvm.Arguments
import technology.idlab.util.Log

abstract class Processor(
    /**
     * The arguments of a processor are stored in a map and can be accessed by name. At the time of
     * writing, the user must manually cast the arguments to the correct type.
     */
    protected val arguments: Arguments,
) {
  /**
   * Processors which wish to log messages should use the logger provided by the template class.
   * This logger is created with the name of the class which extends the template.
   */
  @JvmField protected val log = Log.shared

  abstract suspend fun exec()
}
