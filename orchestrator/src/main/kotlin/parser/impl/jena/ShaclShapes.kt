package technology.idlab.parser.impl.jena

import org.apache.jena.datatypes.xsd.XSDDatatype
import org.apache.jena.rdf.model.Model
import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.vocabulary.SHACLM
import technology.idlab.RDFC
import technology.idlab.extensions.objectOfProperty
import technology.idlab.extensions.subjectWithProperty
import technology.idlab.intermediate.IRParameter
import technology.idlab.parser.ParserException

/**
 * Maps a resource to an IRParameter.Type based on the URI. Note that this implementation is
 * actually quite slow, and we should probably use Apache Jena native APIs here.
 */
private fun Resource.toIRParameterType(): IRParameter.Type {
  return when (this.uri) {
    XSDDatatype.XSDboolean.uri -> IRParameter.Type.BOOLEAN
    XSDDatatype.XSDbyte.uri -> IRParameter.Type.BYTE
    XSDDatatype.XSDdateTime.uri -> IRParameter.Type.DATE
    XSDDatatype.XSDdouble.uri -> IRParameter.Type.DOUBLE
    XSDDatatype.XSDfloat.uri -> IRParameter.Type.FLOAT
    XSDDatatype.XSDint.uri -> IRParameter.Type.INT
    XSDDatatype.XSDdouble.uri -> IRParameter.Type.LONG
    XSDDatatype.XSDstring.uri -> IRParameter.Type.STRING
    RDFC.writer.uri -> IRParameter.Type.WRITER
    RDFC.reader.uri -> IRParameter.Type.READER
    else -> throw ParserException.UnknownDataType(this.uri)
  }
}

/**
 * Create a mapping of String to IRParameter from a SHACL property. This is a recursive
 * implementation that will automatically parse nested classes.
 */
private fun Model.parseSHACLProperty(property: Resource): Pair<String, IRParameter> {
  // Retrieve required fields.
  val minCount = objectOfProperty(property, SHACLM.minCount)?.asLiteral()?.int
  val maxCount = objectOfProperty(property, SHACLM.maxCount)?.asLiteral()?.int
  val node = objectOfProperty(property, SHACLM.node)?.asResource()
  val datatype = objectOfProperty(property, SHACLM.datatype)?.asResource()
  val clazz = objectOfProperty(property, SHACLM.class_)?.asResource()
  val kind = objectOfProperty(property, SHACLM.nodeKind)?.asResource()

  // Retrieve the path of the property.
  val path =
      try {
        objectOfProperty(property, SHACLM.name)!!.asLiteral().string
      } catch (e: Exception) {
        throw ParserException.NoShaclPropertyName(property.uri)
      }

  // Determine the presence.
  val presence =
      if (minCount != null && minCount > 0) {
        IRParameter.Presence.REQUIRED
      } else {
        IRParameter.Presence.OPTIONAL
      }

  // Determine the count.
  val count =
      if (maxCount != null && maxCount == 1) {
        IRParameter.Count.SINGLE
      } else {
        IRParameter.Count.LIST
      }

  // Create a new parameter object.
  val parameter =
      if (kind != null) {
        IRParameter(simple = IRParameter.Type.STRING, presence = presence, count = count)
      } else if (clazz != null) {
        IRParameter(simple = clazz.toIRParameterType(), presence = presence, count = count)
      } else if (datatype != null) {
        IRParameter(simple = datatype.toIRParameterType(), presence = presence, count = count)
      } else if (node != null) {
        IRParameter(complex = parseSHACLShape(node), presence = presence, count = count)
      } else {
        throw ParserException.NoShaclType(property.uri)
      }

  // Return the parameter mapped to its path.
  return Pair(path, parameter)
}

/**
 * Parse a SHACL shape into a mapping of String to IRParameter. This is a recursive implementation
 * that will automatically parse nested classes.
 */
fun Model.parseSHACLShape(shape: Resource): Map<String, IRParameter> {
  val result = mutableMapOf<String, IRParameter>()

  for (property in listObjectsOfProperty(shape, SHACLM.property)) {
    val (key, parameter) = parseSHACLProperty(property.asResource())
    result[key] = parameter
  }

  return result
}

fun Model.isSimpleSHACLShape(path: Resource): Boolean {
  val property =
      subjectWithProperty(SHACLM.path, path) ?: throw ParserException.NoShaclPropertyFound(path.uri)

  val datatype = objectOfProperty(property, SHACLM.datatype)?.asResource()
  val clazz = objectOfProperty(property, SHACLM.class_)?.asResource()
  val kind = objectOfProperty(property, SHACLM.nodeKind)?.asResource()

  if (listOfNotNull(datatype, clazz, kind).size > 1) {
    throw ParserException.ConflictingShaclType(path.uri)
  }

  // A datatype always points to a literal.
  if (datatype != null) {
    return true
  }

  // Specific classes are always simple.
  if (clazz != null && listOf(RDFC.channel, RDFC.reader, RDFC.writer).contains(clazz)) {
    return true
  }

  // If the kind can optionally be a literal, it should be handled as such.
  if (kind != null && kind == SHACLM.IRIOrLiteral) {
    return true
  }

  // Default case: it is a complex object.
  return false
}

fun Model.nameOfSHACLPath(path: Resource): String {
  val property =
      subjectWithProperty(SHACLM.path, path) ?: throw ParserException.NoShaclPropertyFound(path.uri)
  return objectOfProperty(property, SHACLM.name)?.asLiteral()?.string
      ?: throw ParserException.NoShaclPropertyName(path.uri)
}
