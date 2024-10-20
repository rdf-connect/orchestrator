package technology.idlab.rdfc.core.intermediate.parameter

/**
 * Representation of a (list of) literal parameter(s) in IR.
 *
 * @param type The datatype of the parameter.
 */
data class LiteralParameter(
    override val path: String,
    val type: technology.idlab.rdfc.core.intermediate.parameter.LiteralParameterType,
    override val single: Boolean = false,
    override val optional: Boolean = false,
) : technology.idlab.rdfc.core.intermediate.parameter.Parameter
