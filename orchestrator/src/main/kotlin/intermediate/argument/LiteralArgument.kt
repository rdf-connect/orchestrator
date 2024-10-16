package technology.idlab.intermediate.argument

import technology.idlab.intermediate.parameter.LiteralParameter
import technology.idlab.intermediate.parameter.LiteralParameterType

/** Representation of a literal argument in IR as a list of strings. */

/**
 * A literal argument which is represented as a list of strings.
 *
 * @param values The list of values for this argument.
 */
class LiteralArgument(
    override val parameter: LiteralParameter,
    val values: MutableList<String> = mutableListOf(),
) : Argument {
  override fun findAll(type: LiteralParameterType): List<String> {
    return if (parameter.type == type) {
      values
    } else {
      emptyList()
    }
  }
}
