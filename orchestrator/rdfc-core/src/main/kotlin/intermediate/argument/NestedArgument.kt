package technology.idlab.rdfc.core.intermediate.argument

import technology.idlab.rdfc.core.intermediate.parameter.LiteralParameterType
import technology.idlab.rdfc.core.intermediate.parameter.NestedParameter

/**
 * Representation of a single nested argument in IR as a list of key-value pairs.
 *
 * @param values The list of key-value pairs for this argument.
 */
class NestedArgument(
    override val parameter: NestedParameter,
    val values: MutableList<Map<String, Argument>> = mutableListOf(),
) : Argument {
  override fun findAll(type: LiteralParameterType): List<String> {
    /*
     * The function simply loops over all values in the list and recursively calls findAll on each of
     * them.
     */
    val result = mutableListOf<String>()

    for (value in this.values) {
      for ((_, argument) in value) {
        result.addAll(argument.findAll(type))
      }
    }

    return result
  }
}
