package technology.idlab.rdfc.processor

import kotlin.reflect.KProperty
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.typeOf

data class Arguments(
    val args: Map<String, List<Any>>,
) {
  /**
   * Get an argument in a type safe way. The type parameter, either inferred or explicitly given,
   * will be used to recursively check the resulting type. Note that if you want to retrieve an
   * argument with type T which has Argument.Count.REQUIRED, you can either request type T directly
   * or the list with one element using the List<T> type.
   */
  inline operator fun <reified T> get(name: String, strict: Boolean = false): T {
    val type = typeOf<T>()

    // Retrieve the value from the map.
    val argumentList =
        this.args[name]
            ?: run {
              require(type.isMarkedNullable) { "Argument $name not found." }
              return@get null as T
            }

    // Special case: check if the type is not a list, because in that case, we would need to get the
    // first element instead.
    val arg =
        if (T::class.isSuperclassOf(List::class)) {
          argumentList
        } else {
          argumentList.single()
        }

    // Assert the correct type is retrieved.
    require(safeCast(type, arg, strict)) { "Could not parse $name to ${T::class.simpleName}." }
    return arg as T
  }

  inline operator fun <reified T> getValue(thisRef: Any, property: KProperty<*>): T {
    return this.get<T>(property.name)
  }

  companion object {
    /**
     * Parse a (nested) map into type-safe arguments. This method calls itself recursively for all
     * values which are maps as well.
     */
    fun from(args: Map<String, List<Any>>): Arguments {
      return Arguments(
          args.mapValues { (_, list) ->
            list.map { arg ->
              if (arg::class.isSubclassOf(Map::class)) {
                require(safeCast(typeOf<Map<String, List<Any>>>(), arg)) {
                  "Cannot parse nested map."
                }
                @Suppress("UNCHECKED_CAST") (from(arg as Map<String, List<Any>>))
              } else {
                arg
              }
            }
          })
    }
  }
}
