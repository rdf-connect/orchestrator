package technology.idlab.rdfc.intermediate

import technology.idlab.rdfc.intermediate.parameter.Parameter

/**
 * A representation of a parameter in IR.
 *
 * @property type The type of the parameter.
 */
data class IRParameter(val type: Map<String, Parameter>) {
  /**
   * Get the parameter with the given key.
   *
   * @param key The key of the parameter.
   * @return The parameter.
   * @throws IllegalArgumentException If the parameter is not found.
   */
  operator fun get(key: String): Parameter {
    return type[key] ?: throw IllegalArgumentException("No such parameter: $key")
  }
}
