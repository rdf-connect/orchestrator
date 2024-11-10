package technology.idlab.rdfc.intermediate

/**
 * A representation of a pipeline in IR.
 *
 * @property uri The URI of the pipeline.
 * @property dependencies The dependencies of the pipeline. These must be resolved before the
 *   pipeline can be run.
 * @property stages The stages of the pipeline, which are concrete executions of processors and the
 *   arguments they are run with.
 */
data class IRPipeline(
    val uri: String,
    val dependencies: List<IRDependency>,
    val stages: List<IRStage>,
)
