package technology.idlab.intermediate

/** A resolved dependency, with all of its configuration parameters. */
data class IRPackage(
    /** The package's version. */
    val version: String? = null,
    /** The package's author. */
    val author: String? = null,
    /** Description. */
    val description: String? = null,
    /** The URI of the repository. */
    val repo: String? = null,
    /** Source license of the package. */
    val license: String? = null,
    /** The preparation command, which is run before loading the processor. */
    val prepare: List<String> = emptyList(),
    /** The processors inside the package. */
    val processors: List<IRProcessor> = emptyList(),
    /** The runners inside the package. */
    val runners: List<IRRunner> = emptyList(),
)
