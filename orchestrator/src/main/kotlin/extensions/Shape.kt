package technology.idlab.extensions

import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.parser.PropertyShape
import org.apache.jena.shacl.parser.Shape
import org.apache.jena.shacl.vocabulary.SHACLM
import technology.idlab.intermediate.IRParameter

/**
 * Check if the shape is closed.
 *
 * @return true if the shape is closed, false otherwise.
 */
fun Shape.closed(): Boolean {
  return this.constraints.any { it.component.uri == SHACLM.ClosedConstraintComponent.uri }
}

/**
 * Get a property by path.
 *
 * @param path The path as a Resource instance.
 */
fun Shape.property(path: Resource): PropertyShape? {
  return this.propertyShapes.singleOrNull { it.path() == path.uri.toString() }
}

/**
 * Parse a Shape object into a map of IRParameter instances.
 *
 * @return A map of IRParameter instances.
 */
fun Shape.asArguments(): Map<String, IRParameter> {
  val result = mutableMapOf<String, IRParameter>()

  for (property in this.propertyShapes) {
    // Access the property shape.
    val uri = property.path()
    val name = property.name() ?: throw Exception()
    val minCount = property.minCount()
    val maxCount = property.maxCount()
    val datatype = property.datatype() ?: property.class_()
    val node = property.node()

    // Determine if the parameter is optional or required.
    val presence =
        if (minCount == 0) {
          IRParameter.Presence.OPTIONAL
        } else {
          IRParameter.Presence.REQUIRED
        }

    // Determine if the parameter is a list or a single value.
    val count =
        if (maxCount == 1) {
          IRParameter.Count.SINGLE
        } else {
          IRParameter.Count.LIST
        }

    // Create a new IRParameter instance.
    if (datatype == null && node != null) {
      result[name] =
          IRParameter(uri, complex = node.asArguments(), presence = presence, count = count)
    } else if (datatype != null && node == null) {
      result[name] = IRParameter(uri, simple = datatype, presence = presence, count = count)
    } else {
      throw IllegalStateException()
    }
  }

  return result
}
