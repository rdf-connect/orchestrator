package technology.idlab.intermediate.argument

import technology.idlab.intermediate.parameter.LiteralParameterType
import technology.idlab.intermediate.parameter.NestedParameter

/** Representation of a single nested argument in IR as a list of key-value pairs. */
class NestedArgument(
    override val parameter: NestedParameter,
    val values: MutableList<Map<String, Argument>> = mutableListOf(),
) : Argument {
  /*
   * The function simply loops over all values in the list and recursively calls findAll on each of
   * them.
   */
  override fun findAll(type: LiteralParameterType): List<String> {
    val result = mutableListOf<String>()

    for (value in this.values) {
      for ((_, argument) in value) {
        result.addAll(argument.findAll(type))
      }
    }

    return result
  }
}
