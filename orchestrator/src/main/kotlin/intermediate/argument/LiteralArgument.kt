package technology.idlab.intermediate.argument

import technology.idlab.intermediate.parameter.LiteralParameter
import technology.idlab.intermediate.parameter.LiteralParameterType

/** Representation of a literal argument in IR as a list of strings. */
class LiteralArgument(
    override val parameter: LiteralParameter,
    val values: MutableList<String> = mutableListOf(),
) : Argument {
  /*
   * If the argument holds the correct type, we can simply return the whole list.
   */
  override fun findAll(type: LiteralParameterType): List<String> {
    return if (parameter.type == type) {
      values
    } else {
      emptyList()
    }
  }
}
