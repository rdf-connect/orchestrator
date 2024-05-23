package technology.idlab.runner

import java.util.*
import technology.idlab.logging.Log

class Arguments(private val shape: Shape) {
  private val nonNullables = mutableMapOf<String, Any>()
  private val optionals = mutableMapOf<String, Any>()
  private val arrays = mutableMapOf<String, MutableList<Any>>()

  /** Initialize empty arrays for all list variables. */
  init {
    shape.getProperties().forEach { (name, property) ->
      if (property.count == Property.Count.ARRAY) {
        arrays[name] = mutableListOf()
      }
    }
  }

  /** Add a parsed argument to the builder by name. */
  fun add(name: String, value: Any) {
    when (shape.getProperty(name).count) {
      Property.Count.NON_NULL -> nonNullables[name] = value
      Property.Count.OPTIONAL -> optionals[name] = value
      Property.Count.ARRAY -> {
        val list = arrays[name] ?: Log.shared.fatal("Array $name not found")
        list.add(value)
      }
    }
  }

  fun toMap(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()

    // Add all non-nullables.
    result.putAll(nonNullables)

    // Insert an optional version of each optional argument.
    result.putAll(optionals.mapValues { Optional.of(it.value) })

    // Mark the not set optionals explicitly.
    this.shape.getProperties().forEach {
      // Check if is optional.
      if (it.value.count != Property.Count.OPTIONAL) {
        return@forEach
      }

      result.getOrPut(it.key) { Optional.empty<Any>() }
    }

    // Add all lists. All lists are instantiated, so no need to go over the
    // empty ones.
    result.putAll(arrays)

    return result
  }
}
