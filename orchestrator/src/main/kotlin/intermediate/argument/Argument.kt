package technology.idlab.intermediate.argument

import technology.idlab.intermediate.parameter.LiteralParameterType
import technology.idlab.intermediate.parameter.Parameter

/** IR representation of a concrete argument. It holds it's corresponding parameter as a field. */
sealed interface Argument {
  // The parameter that this argument corresponds to.
  val parameter: Parameter

  /**
   * Find all values of a certain type for a given parameter.
   *
   * @param type The type of the literal parameter to find.
   * @return A flattened list of all values of the given type for the given parameter.
   */
  fun findAll(type: LiteralParameterType): List<String>
}
