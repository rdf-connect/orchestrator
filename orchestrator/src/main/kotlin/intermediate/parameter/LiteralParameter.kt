package technology.idlab.intermediate.parameter

/** Representation of a (list of) literal parameter(s) in IR. */
data class LiteralParameter(
    // The path of the parameter as specified in the config.
    override val path: String,
    // The datatype of the parameter.
    val type: LiteralParameterType,
    // True if there is only one instance of the parameter at most.
    override val single: Boolean = false,
    // True if there may be zero instances of the parameter.
    override val optional: Boolean = false,
) : Parameter
