package technology.idlab.intermediate

import java.io.File
import technology.idlab.intermediate.runner.RunnerType

data class IRRunner(
    val uri: String,
    val directory: File? = null,
    val entrypoint: String? = null,
    val type: RunnerType,
)
