package technology.idlab.rdfc.processor

import technology.idlab.rdfc.core.log.Log

/**
 * Processors are the building blocks of RDF-Connect. They are responsible for executing the actual
 * logic inside a pipeline. This class models a processor targeting the JVMRunner
 */
abstract class Processor(
    /**
     * The arguments of a processor are stored in a map and can be accessed by name. Type safety is
     * assured through the use of Kotlin reflection.
     */
    protected val arguments: Arguments,
) {
  @JvmField val log = Log.shared

  /**
   * The `exec` function will be called when the pipeline executes. All inter-processor
   * communication must happen here, and not in the constructor.
   */
  abstract suspend fun exec()
}
