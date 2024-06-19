package technology.idlab.parser.intermediate

data class IRArgument(
    // Parameter name.
    val name: String,
    // Concrete but unparsed value.
    val value: List<String>,
)
