package technology.idlab.intermediate.parameter

/** Representation of nested parameter in IR. */
open class NestedParameter(
    override val path: String,
    val type: Map<String, Parameter>,
    override val single: Boolean = false,
    override val optional: Boolean = false
) : Parameter {
  /**
   * Get the parameter with the given key.
   *
   * @param key The key of the parameter.
   * @return The parameter.
   */
  operator fun get(key: String): Parameter {
    return type[key] ?: throw IllegalArgumentException("Parameter $key not found.")
  }
}
