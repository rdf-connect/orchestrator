package technology.idlab.rdfc.core.intermediate

/**
 * A resolved dependency, with all of its configuration parameters.
 *
 * @property version The version of the dependency.
 * @property author The author of the dependency.
 * @property description The description of the dependency.
 * @property repo The URI of the repository.
 * @property license The license of the dependency.
 * @property prepare The preparation command, which is run before loading the processor.
 * @property processors The processors inside the dependency.
 * @property runners The runners inside the dependency.
 */
data class IRPackage(
    val version: String? = null,
    val author: String? = null,
    val description: String? = null,
    val repo: String? = null,
    val license: String? = null,
    val prepare: List<String> = emptyList(),
    val processors: List<technology.idlab.rdfc.core.intermediate.IRProcessor> = emptyList(),
    val runners: List<technology.idlab.rdfc.core.intermediate.IRRunner> = emptyList(),
)
