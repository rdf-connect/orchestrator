package technology.idlab.intermediate

import technology.idlab.intermediate.argument.Argument
import technology.idlab.intermediate.parameter.LiteralParameterType

class IRArgument(
    val root: Map<String, Argument> = mutableMapOf(),
) {
  /**
   * Get the argument for a given key.
   *
   * @param key The key of the argument.
   * @return The argument.
   * @throws IllegalArgumentException If the argument is not found.
   */
  operator fun get(key: String): Argument {
    return root[key] ?: throw IllegalArgumentException("Argument $key not found.")
  }

  /**
   * Find all values of a certain type for a given parameter.
   *
   * @param type The type of the literal parameter to find.
   * @result A flattened list of all values of the given type for the given parameter.
   */
  fun findAll(type: LiteralParameterType): List<String> {
    return root.values.map { it.findAll(type) }.flatten()
  }
}
