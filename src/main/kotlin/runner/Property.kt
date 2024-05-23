package technology.idlab.runner

class Property(val name: String, val type: String, min: Int?, max: Int?) {
  enum class Count {
    NON_NULL,
    OPTIONAL,
    ARRAY,
  }

  /**
   * Determine the wrapper type of the property, which is either the type itself, an optional type,
   * or an array of the type.
   */
  val count =
      if (min == 1 && max == 1) {
        Count.NON_NULL
      } else if (max == 1) {
        Count.OPTIONAL
      } else {
        Count.ARRAY
      }
}
