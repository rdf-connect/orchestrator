package technology.idlab.intermediate

data class IRPipeline(
    val uri: String,
    val dependencies: List<IRDependency>,
    val stages: List<IRStage>,
)
