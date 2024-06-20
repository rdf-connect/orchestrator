package technology.idlab.parser.intermediate

data class IRStage(
    // The URI of the stage.
    val uri: String,
    // The processor that the stage is associated with.
    val processor: IRProcessor,
    // Concrete but unparsed arguments for the stage.
    val arguments: List<IRArgument> = emptyList(),
)
