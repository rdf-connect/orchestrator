package technology.idlab.intermediate.parameter

/**
 * Representation of a (list of) literal parameter(s) in IR.
 *
 * @param type The datatype of the parameter.
 */
data class LiteralParameter(
    override val path: String,
    val type: LiteralParameterType,
    override val single: Boolean = false,
    override val optional: Boolean = false,
) : Parameter
