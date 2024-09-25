package technology.idlab.intermediate

/**
 * Intermediate representation of an argument. These can be either simple or complex, meaning they
 * contain either a list of values or a map of key-value pairs. Note that it doesn't matter whether
 * the presence of an argument is required or optional, as well as whether it is a single value or a
 * list of values. This is because the parser should have already taken care of these details.
 */
data class IRArgument(
    // In case of simple: concrete but unparsed value.
    private val simple: List<String>? = null,
    // In case of complex: list of key-value pairs.
    private val complex: List<Map<String, IRArgument>>? = null,
    // The shape of the argument.
    val parameter: IRParameter,
) {
  init {
    if (simple == null && complex == null) {
      throw IllegalStateException()
    }

    if (simple != null && complex != null) {
      throw IllegalStateException()
    }
  }

  fun getSimple(): List<String> {
    return simple ?: throw IllegalStateException()
  }

  fun getComplex(): List<Map<String, IRArgument>> {
    return complex ?: throw IllegalStateException()
  }
}
