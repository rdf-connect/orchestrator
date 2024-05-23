package technology.idlab.runner

import java.util.*
import technology.idlab.logging.Log

enum class PropertyCount {
  NON_NULL,
  OPTIONAL,
  ARRAY,
}

class Property(val name: String, val type: String, min: Int?, max: Int?) {
  /**
   * Determine the wrapper type of the property, which is either the type itself, an optional type,
   * or an array of the type.
   */
  val count: PropertyCount =
      if (min == 1 && max == 1) {
        PropertyCount.NON_NULL
      } else if (max == 1) {
        PropertyCount.OPTIONAL
      } else {
        PropertyCount.ARRAY
      }
}

class Shape {
  private val properties = mutableMapOf<String, Property>()

  fun addProperty(name: String, type: String, min: Int?, max: Int?) {
    properties[name] = Property(name, type, min, max)
  }

  fun getProperty(name: String): Property {
    return properties[name] ?: Log.shared.fatal("Property $name not found")
  }

  fun getProperties(): Map<String, Property> {
    return properties
  }
}

class ArgumentBuilder(private val shape: Shape) {
  private val nonNullables = mutableMapOf<String, Any>()
  private val optionals = mutableMapOf<String, Any>()
  private val arrays = mutableMapOf<String, MutableList<Any>>()

  /** Initialize empty arrays for all list variables. */
  init {
    shape.getProperties().forEach { (name, property) ->
      if (property.count == PropertyCount.ARRAY) {
        arrays[name] = mutableListOf()
      }
    }
  }

  /** Add a parsed argument to the builder by name. */
  fun add(name: String, value: Any) {
    when (shape.getProperty(name).count) {
      PropertyCount.NON_NULL -> nonNullables[name] = value
      PropertyCount.OPTIONAL -> optionals[name] = value
      PropertyCount.ARRAY -> {
        val list = arrays[name] ?: Log.shared.fatal("Array $name not found")
        list.add(value)
      }
    }
  }

  fun build(): Map<String, Any> {
    val result = mutableMapOf<String, Any>()

    // Add all non-nullables.
    result.putAll(nonNullables)

    // Insert an optional version of each optional argument.
    result.putAll(optionals.mapValues { Optional.of(it.value) })

    // Mark the not set optionals explicitly.
    this.shape.getProperties().forEach {
      if (it.value.count != PropertyCount.OPTIONAL) {
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
