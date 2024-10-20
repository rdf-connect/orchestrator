package technology.idlab.rdfc.core.intermediate.parameter

/**
 * Representation of nested parameter in IR.
 *
 * @param type The map of parameters.
 */
open class NestedParameter(
    override val path: String,
    val type: Map<String, technology.idlab.rdfc.core.intermediate.parameter.Parameter>,
    override val single: Boolean = false,
    override val optional: Boolean = false
) : technology.idlab.rdfc.core.intermediate.parameter.Parameter {
  /**
   * Get the parameter with the given key.
   *
   * @param key The key of the parameter.
   * @return The parameter.
   */
  operator fun get(key: String): technology.idlab.rdfc.core.intermediate.parameter.Parameter {
    val result = type[key]
    check(result != null) { "Parameter $key not found." }
    return result
  }
}
