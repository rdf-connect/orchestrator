package technology.idlab.parser

import technology.idlab.logging.Log

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

  fun getBuilder(): Arguments {
    return Arguments(this)
  }
}
