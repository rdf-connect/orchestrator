package sources

import java.io.File
import java.util.Optional
import kotlin.test.Test
import kotlin.test.assertEquals
import technology.idlab.runner.Pipeline

class OptionalsTest {
  @Test
  fun parameters() {
    val configPath = this.javaClass.getResource("/pipelines/optionals.ttl")
    val config = File(configPath!!.path)
    val pipeline = Pipeline(config)
    val processors = pipeline.processors

    // Get the sole processor in the pipeline.
    assertEquals(processors.size, 1)
    val processor = processors[0]

    // Retrieve arguments.
    val required: String = processor.getArgument("required")
    val present: Optional<String> = processor.getOptionalArgument("present")
    val missing: Optional<String> = processor.getOptionalArgument("missing")

    // Check if results are correct.
    assertEquals("A Required String", required)
    assertEquals("An Optional String", present.get())
    assertEquals(Optional.empty<String>(), missing)
  }
}
