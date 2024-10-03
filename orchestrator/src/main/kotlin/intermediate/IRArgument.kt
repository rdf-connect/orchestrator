package technology.idlab.intermediate

sealed interface Argument {
  val parameter: Parameter

  fun findAll(type: LiteralParameterType, parameter: Parameter): List<String>
}

class LiteralArgument(
    override val parameter: LiteralParameter,
    val values: MutableList<String> = mutableListOf(),
) : Argument {
  override fun findAll(type: LiteralParameterType, parameter: Parameter): List<String> {
    if (parameter !is LiteralParameter) {
      return emptyList()
    }

    return if (parameter.type == type) {
      this.values
    } else {
      emptyList()
    }
  }
}

class NestedArgument(
    override val parameter: NestedParameter,
    val values: MutableList<Map<String, Argument>> = mutableListOf(),
) : Argument {
  override fun findAll(type: LiteralParameterType, parameter: Parameter): List<String> {
    if (parameter !is NestedParameter) {
      throw IllegalStateException()
    }

    val result = mutableListOf<String>()

    for (value in this.values) {
      for ((key, argument) in value) {
        result.addAll(argument.findAll(type, parameter[key]))
      }
    }

    return result
  }
}

class IRArgument(
    val parameter: IRParameter,
    val values: Map<String, Argument> = mutableMapOf(),
) {
  operator fun get(key: String): Argument {
    return values[key] ?: throw IllegalArgumentException("Argument $key not found.")
  }

  fun findAll(type: LiteralParameterType, parameter: Parameter): List<String> {
    return this.values.values.map { it.findAll(type, parameter) }.flatten()
  }
}
