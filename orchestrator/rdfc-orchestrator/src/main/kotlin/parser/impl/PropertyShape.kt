package technology.idlab.parser.impl

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.shacl.Shapes
import org.apache.jena.shacl.parser.PropertyShape
import org.apache.jena.shacl.parser.Shape
import org.apache.jena.shacl.vocabulary.SHACLM
import technology.idlab.parser.exception.MissingShaclPathException
import technology.idlab.parser.exception.UnknownDataTypeException
import technology.idlab.rdfc.core.RDFC
import technology.idlab.rdfc.core.intermediate.parameter.LiteralParameterType

/**
 * Get the maxCount constraint of a property shape.
 *
 * @return The maxCount constraint.
 */
fun PropertyShape.maxCount(): Int? {
  // Find the maxCount constraint.
  val result = this.shapeGraph.find(this.shapeNode, SHACLM.maxCount.asNode(), null)

  // There should be only one maxCount constraint.
  val node = result.toList().singleOrNull()

  // Return the maxCount value.
  return node?.`object`?.literal?.value?.toString()?.toInt()
}

/**
 * Get the minCount constraint of a property shape.
 *
 * @return The minCount constraint.
 */
fun PropertyShape.minCount(): Int? {
  // Find the minCount constraint.
  val result = this.shapeGraph.find(this.shapeNode, SHACLM.minCount.asNode(), null)

  // There should be only one minCount constraint.
  val node = result.toList().singleOrNull()

  // Return the minCount value.
  return node?.`object`?.literal?.value?.toString()?.toInt()
}

/**
 * Get the name constraint of a property shape.
 *
 * @return The name constraint.
 */
fun PropertyShape.name(): String? {
  // Find the name constraint.
  val result = this.shapeGraph.find(this.shapeNode, SHACLM.name.asNode(), null)

  // There should be only one name constraint.
  val node = result.toList().singleOrNull()

  // Return the name value.
  return node?.`object`?.literal?.value?.toString()
}

/**
 * Attempt to parse a property into its own shape.
 *
 * @return The shape of the property.
 */
fun PropertyShape.node(): Shape? {
  val result = this.shapeGraph.find(this.shapeNode, SHACLM.node.asNode(), null)

  // There should be only one node constraint.
  val node = result.toList().singleOrNull() ?: return null

  // Return the shape.
  val shapes = Shapes.parse(this.shapeGraph)
  return shapes.getShape(node.`object`)
}

/**
 * Get the datatype constraint of a property shape.
 *
 * @return The datatype constraint.
 */
fun PropertyShape.datatype(): LiteralParameterType? {
  val nodeQuery = this.shapeGraph.find(this.shapeNode, SHACLM.datatype.asNode(), null)

  // There should be only one datatype constraint.
  val node = nodeQuery.toList().singleOrNull() ?: return null

  return when (node.`object`.uri) {
    XSDDatatype.XSDboolean.uri -> LiteralParameterType.BOOLEAN
    XSDDatatype.XSDbyte.uri -> LiteralParameterType.BYTE
    XSDDatatype.XSDdateTime.uri -> LiteralParameterType.DATE
    XSDDatatype.XSDdouble.uri -> LiteralParameterType.DOUBLE
    XSDDatatype.XSDfloat.uri -> LiteralParameterType.FLOAT
    XSDDatatype.XSDint.uri -> LiteralParameterType.INT
    XSDDatatype.XSDdouble.uri -> LiteralParameterType.LONG
    XSDDatatype.XSDstring.uri -> LiteralParameterType.STRING
    RDFC.writer.uri -> LiteralParameterType.WRITER
    RDFC.reader.uri -> LiteralParameterType.READER
    else -> throw UnknownDataTypeException(node.`object`.uri)
  }
}

/** @return The path of the property as a URI. */
fun PropertyShape.path(): String {
  val result = this.shapeGraph.find(this.shapeNode, SHACLM.path.asNode(), null)

  // There should be only one path constraint.
  val node = result.toList().singleOrNull() ?: throw MissingShaclPathException()

  return node.`object`.toString()
}

/** @return The parameter type of the property shape. */
fun PropertyShape.clazz(): LiteralParameterType? {
  val classQuery = this.shapeGraph.find(this.shapeNode, SHACLM.class_.asNode(), null)
  val kindQuery = this.shapeGraph.find(this.shapeNode, SHACLM.nodeKind.asNode(), null)

  // There should be only one class value.
  val node = classQuery.toList().singleOrNull() ?: kindQuery.toList().singleOrNull() ?: return null

  return when (node.`object`.uri) {
    RDFC.writer.uri -> LiteralParameterType.WRITER
    RDFC.reader.uri -> LiteralParameterType.READER
    SHACLM.IRIOrLiteral.uri -> LiteralParameterType.STRING
    else -> throw UnknownDataTypeException(node.`object`.uri)
  }
}
