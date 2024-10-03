package technology.idlab.intermediate.parameter

/**
 * Generic representation of a parameter in IR, which may be either a literal or a nested parameter.
 */
sealed interface Parameter {
  // The path of the parameter as specified in the config.
  val path: String
  // True if there is only one instance of the parameter at most.
  val single: Boolean
  // True if there may be zero instances of the parameter.
  val optional: Boolean
  // True if there can be more than one instance of the parameter.
  val list: Boolean
    get() = !single

  // True if the parameter is not optional.
  val required: Boolean
    get() = !optional
}
