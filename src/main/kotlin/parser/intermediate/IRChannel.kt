package technology.idlab.parser.intermediate

data class IRChannel(
    // The URI of the channel.
    val uri: String,
    // The URI of the data source (writer).
    val source: String,
    // The URI of the data destination (reader).
    val destination: String,
)
