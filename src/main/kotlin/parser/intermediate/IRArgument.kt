package technology.idlab.parser.intermediate

data class IRArgument(
    // Parameter.
    val parameter: IRParameter,
    // Concrete but unparsed value.
    val value: List<String>,
)
