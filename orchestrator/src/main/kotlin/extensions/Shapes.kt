package technology.idlab.extensions

import org.apache.jena.rdf.model.Resource
import org.apache.jena.shacl.Shapes
import org.apache.jena.shacl.parser.Shape

/**
 * Get a list of all the shapes which target a specific resource.
 *
 * @param target The resource the shapes should be targeting.
 */
fun Shapes.targeting(target: Resource): List<Shape> {
  return this.targetShapes.filter { shape -> shape.targets.any { it.`object`.uri == target.uri } }
}
