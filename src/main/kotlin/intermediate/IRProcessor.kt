package technology.idlab.intermediate

class IRProcessor(
    /** The URI of the processor. */
    val uri: String,
    /** The destination of the processor. */
    val target: String,
    /** The entrypoint. */
    val entrypoint: String? = null,
    /** Processor parameters. */
    val parameters: Map<String, IRParameter> = emptyMap(),
    /** Additional parameters. These may be used by the runner for any reason. */
    val metadata: Map<String, String> = emptyMap()
)
