package technology.idlab.rdfc.core.intermediate

/**
 * A representation of a processor in IR. A processor is a piece of code which can be executed by a
 * runner and has access to specific parameters.
 *
 * @property uri The URI of the processor.
 * @property target The URI of the runner which should execute this processor.
 * @property entrypoint The location of the entrypoint.
 * @property parameters The parameters which should be passed to the processor.
 * @property metadata Additional parameters which may be used by the runner.
 */
class IRProcessor(
    val uri: String,
    val target: String,
    val entrypoint: String,
    val parameters: IRParameter,
    val metadata: Map<String, String> = emptyMap()
)
