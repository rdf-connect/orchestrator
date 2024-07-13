package technology.idlab.intermediate

import java.io.File

data class IRRunner(
    val uri: String,
    val directory: File? = null,
    val entrypoint: String? = null,
    val type: IRRunner.Type,
) {
  enum class Type {
    GRPC,
    BUILT_IN,
  }
}
