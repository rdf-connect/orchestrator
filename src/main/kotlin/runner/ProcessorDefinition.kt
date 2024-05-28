package technology.idlab.runner

import java.io.File
import technology.idlab.logging.Log
import technology.idlab.util.classesWithAnnotation

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
annotation class ProcessorDefinition(val resource: String) {
  data class Config(val clazz: Class<out Processor>, val ontology: File, val uri: String)

  companion object {
    fun scan(): List<Config> {
      return classesWithAnnotation(ProcessorDefinition::class.java)
          .map {
            try {
              it.asSubclass(Processor::class.java)
            } catch (e: ClassCastException) {
              Log.shared.fatal("Class $it is not a Processor")
            }
          }
          .map {
            val annotation = it.getAnnotation(ProcessorDefinition::class.java)

            val resource =
                it.getResource(annotation.resource)
                    ?: Log.shared.fatal("Resource ${annotation.resource} not found")

            Config(it, File(resource.toURI()), "https://w3id.org/conn/jvm#${it.simpleName}")
          }
    }
  }
}
