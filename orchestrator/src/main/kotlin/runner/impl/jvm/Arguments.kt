package technology.idlab.runner.impl.jvm

import kotlin.reflect.KProperty
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmErasure
import kotlin.reflect.typeOf
import technology.idlab.util.Log

/**
 * Recursively check if a value corresponds to a given KType. This function can be run either
 * strictly or loosely. This is due to type erasure, which means we must cast the value to the KType
 * and deal with type parameters manually. For example, for Pair<A, B>, we call the function
 * recursively for both the `first` as `second` data field. For List<T>, we call the function for
 * each element of the list. If a given container is not supported in that fashion, the result will
 * depend on the strict parameter.
 */
fun safeCast(to: KType, from: Any, strict: Boolean = false): Boolean {
  // The base case, where the Any type matches everything.
  if (to.jvmErasure == Any::class) {
    return true
  }

  // The requested type must be an actual superclass of the value given.
  if (!to.jvmErasure.isSuperclassOf(from::class)) {
    return false
  }

  // Retrieve the type arguments. If these are empty, then we can safely assume that the type is
  // cast correctly and safely.
  val typeArguments = to.arguments
  if (typeArguments.isEmpty()) {
    return true
  }

  // If the value is pair, check both first and second.
  if (to.jvmErasure == Pair::class) {
    if (!from::class.isSubclassOf(Pair::class)) {
      return false
    }

    // Extract pair and the type arguments.
    @Suppress("UNCHECKED_CAST") val pair = from as Pair<Any, Any>
    val first = to.arguments[0].type!!
    val second = to.arguments[1].type!!

    return safeCast(first, pair.first) && safeCast(second, pair.second)
  }

  // If the value is a list, check all elements.
  if (to.jvmErasure == List::class) {
    if (!from::class.isSubclassOf(List::class)) {
      return false
    }

    // Extract values.
    @Suppress("UNCHECKED_CAST") val list = from as List<Any>
    val elementType = to.arguments[0].type!!
    return list.all { safeCast(elementType, it, strict) }
  }

  // We will never be able to exhaustively go over all types, due to type erasure. However, we're
  // if the user is okay with non-strict type checking, we may end here.
  return !strict
}

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
            ?: if (type.isMarkedNullable) {
              return null as T
            } else {
              Log.shared.fatal("Argument $name is missing")
            }

    // Special case: check if the type is not a list, because in that case, we would need to get the
    // first element instead.
    val arg =
        if (T::class.isSuperclassOf(List::class)) {
          argumentList
        } else {
          if (argumentList.size != 1) {
            Log.shared.fatal("Cannot obtain single argument if there is not exactly one value.")
          }

          argumentList[0]
        }

    if (safeCast(type, arg, strict)) {
      return arg as T
    } else {
      Log.shared.fatal("Could not parse $name to ${T::class.simpleName}")
    }
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
                if (safeCast(typeOf<Map<String, List<Any>>>(), arg)) {
                  @Suppress("UNCHECKED_CAST") (from(arg as Map<String, List<Any>>))
                } else {
                  Log.shared.fatal("Cannot have raw maps in arguments.")
                }
              } else {
                arg
              }
            }
          })
    }
  }
}
