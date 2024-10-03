package technology.idlab.intermediate

import technology.idlab.intermediate.parameter.LiteralParameterType

data class IRStage(
    // The URI of the stage.
    val uri: String,
    // The processor.
    val processor: IRProcessor,
    // Concrete but unparsed arguments for the stage.
    val arguments: IRArgument,
) {
  fun readers(): List<String> {
    return arguments.findAll(LiteralParameterType.READER)
  }

  fun writers(): List<String> {
    return arguments.findAll(LiteralParameterType.WRITER)
  }
}
