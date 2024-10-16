package technology.idlab.intermediate.argument

import technology.idlab.intermediate.parameter.LiteralParameterType
import technology.idlab.intermediate.parameter.Parameter

/**
 * A concrete argument and it's corresponding parameter.
 *
 * @property parameter The parameter that this argument corresponds to.
 */
sealed interface Argument {
  val parameter: Parameter

  /**
   * Find all values of a certain type for a given parameter.
   *
   * @param type The type of the literal parameter to find.
   * @return A flattened list of all values of the given type for the given parameter.
   */
  fun findAll(type: LiteralParameterType): List<String>
}
