package technology.idlab.intermediate

import technology.idlab.intermediate.parameter.Parameter

class IRParameter(
    val type: Map<String, Parameter>,
) {
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
