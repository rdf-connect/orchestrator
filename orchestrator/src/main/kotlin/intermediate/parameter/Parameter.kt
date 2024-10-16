package technology.idlab.intermediate.parameter

/**
 * Representation of a (list of) literal parameter(s) in IR.
 *
 * @property path The path of the parameter as specified in the config.
 * @property single True if there is only one instance of the parameter at most.
 * @property optional True if there may be zero instances of the parameter.
 * @property list True if there can be more than one instance of the parameter.
 * @property required True if the parameter is not optional.
 */
sealed interface Parameter {
  val path: String
  val single: Boolean
  val optional: Boolean
  val list: Boolean
    get() = !single

  val required: Boolean
    get() = !optional
}
