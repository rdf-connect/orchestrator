package technology.idlab.runner.impl.jvm

import technology.idlab.log.Log

abstract class Processor(
    /**
     * The arguments of a processor are stored in a map and can be accessed by name. Type safety is
     * assured through the use of Kotlin reflection.
     */
    protected val arguments: Arguments,
) {
  /**
   * Processors which wish to log messages should use the logger provided by the template class.
   * This logger is created with the name of the class which extends the template.
   */
  @JvmField protected val log = Log.shared

  /**
   * The `exec` function will be called when the pipeline executes. All inter-processor
   * communication must happen here, and not in the constructor.
   */
  abstract suspend fun exec()
}
