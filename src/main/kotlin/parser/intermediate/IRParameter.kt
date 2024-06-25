package technology.idlab.parser.intermediate

data class IRParameter(
    // The type of the argument.
    val type: Type,
    // Whether the argument is required.
    val presence: Presence,
    // Whether the argument is a single value or a list of values.
    val count: Count,
) {
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
}
