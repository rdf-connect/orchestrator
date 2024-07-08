package technology.idlab.parser.intermediate

data class IRPipeline(
    val uri: String,
    val dependencies: List<IRDependency>,
    val stages: List<IRStage>,
)
