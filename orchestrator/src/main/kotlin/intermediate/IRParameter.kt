package technology.idlab.intermediate

data class IRParameter(
    // In case of simple: concrete but unparsed value.
    private val simple: Type? = null,
    // In case of complex: list of key-value pairs.
    private val complex: Map<String, IRParameter>? = null,
    // Whether the argument is required.
    val presence: Presence,
    // Whether the argument is a single value or a list of values.
    val count: Count,
) {
  enum class Kind {
    SIMPLE,
    COMPLEX,
  }

  /* The data type of the argument. */
  enum class Type {
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

  /* Whether the argument is required or not.*/
  enum class Presence {
    REQUIRED,
    OPTIONAL,
  }

  /**
   * The number of values that the argument can take. Either a single value, or a list of values.
   */
  enum class Count {
    SINGLE,
    LIST,
  }

  val kind = if (simple != null) Kind.SIMPLE else Kind.COMPLEX

  init {
    assert(simple != null || complex != null)
    assert(simple == null || complex == null)
  }

  fun getSimple(): Type {
    return simple ?: throw IllegalStateException("IRParameter is not simple.")
  }

  fun getComplex(): Map<String, IRParameter> {
    return complex ?: throw IllegalStateException("IRParameter is not complex.")
  }

  operator fun get(key: String): IRParameter {
    return complex?.get(key) ?: throw IllegalStateException("IRParameter is not complex.")
  }
}
