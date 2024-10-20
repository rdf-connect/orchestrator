package technology.idlab.rdfc.core.intermediate

import technology.idlab.rdfc.core.intermediate.parameter.LiteralParameterType

/**
 * Representation of a stage in the IR. A stage is an execution of a processor with concrete
 * arguments.
 *
 * @property uri The URI of the stage.
 * @property processor The processor which should be executed for this stage.
 * @property arguments Concrete but unparsed arguments for the stage.
 */
data class IRStage(
    val uri: String,
    val processor: technology.idlab.rdfc.core.intermediate.IRProcessor,
    val arguments: technology.idlab.rdfc.core.intermediate.IRArgument,
) {
  /**
   * Get all the readers for this stage.
   *
   * @return A list of URIs as strings.
   */
  fun readers(): List<String> {
    return arguments.findAll(LiteralParameterType.READER)
  }

  /**
   * Get all the writers for this stage.
   *
   * @return A list of URIs as strings.
   */
  fun writers(): List<String> {
    return arguments.findAll(LiteralParameterType.WRITER)
  }
}
