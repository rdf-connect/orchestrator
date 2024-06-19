package technology.idlab.parser.intermediate

import technology.idlab.runtime.Runner

class IRProcessor(
    // The URI of the processor.
    val uri: String,
    // The destination of the processor.
    val target: Runner.Target,
    // Processor parameters.
    val parameters: List<IRParameter> = emptyList(),
    // Additional parameters. These may be used by the runner for any reason.
    val metadata: Map<String, String> = emptyMap()
) {
  /** Load the abstract processor in the runner. */
  suspend fun prepare() {
    Runner.get(target).prepare(this)
  }
}
