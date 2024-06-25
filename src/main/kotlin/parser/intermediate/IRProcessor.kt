package technology.idlab.parser.intermediate

import runner.Runner

class IRProcessor(
    // The URI of the processor.
    val uri: String,
    // The destination of the processor.
    val target: Runner.Target,
    // Processor parameters.
    val parameters: Map<String, IRParameter> = emptyMap(),
    // Additional parameters. These may be used by the runner for any reason.
    val metadata: Map<String, String> = emptyMap()
)
