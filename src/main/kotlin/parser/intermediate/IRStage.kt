package technology.idlab.parser.intermediate

import technology.idlab.runtime.Runner

data class IRStage(
    // The URI of the stage.
    val uri: String,
    // The processor that the stage is associated with.
    val processor: IRProcessor,
    // Concrete but unparsed arguments for the stage.
    val arguments: List<IRArgument> = emptyList(),
) {
  /** Load the abstract stage in the runner. */
  suspend fun prepare() {
    Runner.get(processor.target).prepare(this)
  }
}
