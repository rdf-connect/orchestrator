package technology.idlab.util

import org.reflections.Reflections
import org.reflections.scanners.Scanners
import org.reflections.util.ConfigurationBuilder

internal fun classesWithAnnotation(annotation: Class<out Annotation>): Set<Class<*>> {
  // Get a list of all classes with type annotations.
  val config =
      ConfigurationBuilder()
          .forPackages("") // Root package, adjust if needed
          .addScanners(Scanners.TypesAnnotated)

  // Initialize a Reflections client.
  val reflections = Reflections(config)

  // Filter based on JVMRunnerProcessor
  return reflections.getTypesAnnotatedWith(annotation)
}
