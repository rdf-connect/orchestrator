package technology.idlab.intermediate

class IRParameter(
    uri: String,
    type: Map<String, Parameter>,
) :
    NestedParameter(
        uri,
        type,
        single = false,
        optional = false,
    )

sealed interface Parameter {
  val uri: String
  val single: Boolean
  val optional: Boolean
}

open class NestedParameter(
    override val uri: String,
    val type: Map<String, Parameter>,
    override val single: Boolean = false,
    override val optional: Boolean = false
) : Parameter {
  /**
   * Get the parameter with the given key.
   *
   * @param key The key of the parameter.
   * @return The parameter.
   */
  operator fun get(key: String): Parameter {
    return type[key] ?: throw IllegalArgumentException("Parameter $key not found.")
  }
}

enum class LiteralParameterType {
  BOOLEAN,
  BYTE,
  DATE,
  DOUBLE,
  FLOAT,
  INT,
  LONG,
  STRING,
  WRITER,
  READER,
}

data class LiteralParameter(
    override val uri: String,
    val type: LiteralParameterType,
    override val single: Boolean = false,
    override val optional: Boolean = false,
) : Parameter
