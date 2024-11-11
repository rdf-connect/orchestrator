package technology.idlab.rdfc.processor

import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.isSuperclassOf
import kotlin.reflect.jvm.jvmErasure

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
