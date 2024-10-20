package technology.idlab.rdfc.core.intermediate

import java.io.File
import technology.idlab.rdfc.core.intermediate.runner.RunnerType

/**
 * Representation of a runner in IR.
 *
 * @param uri The URI of the runner.
 * @param directory The directory in which the implementation is located.
 * @param entrypoint The directory in which the commands listed in the configuration should be run.
 * @param type The type of the runner, as defined in [RunnerType].
 */
data class IRRunner(
    val uri: String,
    val directory: File? = null,
    val entrypoint: String? = null,
    val type: RunnerType,
)
