package technology.idlab.extensions

import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.parser.PropertyShape
import org.apache.jena.shacl.parser.Shape
import org.apache.jena.shacl.vocabulary.SHACLM
import technology.idlab.intermediate.parameter.LiteralParameter
import technology.idlab.intermediate.parameter.NestedParameter
import technology.idlab.intermediate.parameter.Parameter

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
fun Shape.asArguments(): Map<String, Parameter> {
  val result = mutableMapOf<String, Parameter>()

  for (property in this.propertyShapes) {
    // Access the property shape.
    val uri = property.path()
    val name = property.name() ?: throw Exception()
    val minCount = property.minCount()
    val maxCount = property.maxCount()
    val datatype = property.datatype() ?: property.class_()
    val node = property.node()

    // Determine if the parameter is optional or required.
    val optional = minCount == 0

    // Determine if the parameter is a list or a single value.
    val count = maxCount == 1

    // Create a new IRParameter instance.
    if (node != null) {
      check(datatype == null) { "A node cannot have a datatype." }
      result[name] = NestedParameter(uri, node.asArguments(), single = count, optional = optional)
    } else {
      check(datatype != null) { "A property must have a datatype." }
      result[name] = LiteralParameter(uri, datatype, single = count, optional = optional)
    }
  }

  return result
}
