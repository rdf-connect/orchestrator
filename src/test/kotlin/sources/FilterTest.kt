package sources

import java.io.File
import kotlin.test.Test
import technology.idlab.runner.Pipeline

class FilterTest {
  @Test
  fun parameters() {
    val configPath = this.javaClass.getResource("/pipelines/filter.ttl")
    val config = File(configPath!!.path)
    val pipeline = Pipeline(config)
    val processors = pipeline.processors

    // Get the sole processor in the pipeline.
    assert(processors.size == 1)
    val filter = processors[0]

    // Check arguments.
    val whitelist: List<Int> = filter.getArgument("whitelist")
    assert(whitelist.size == 5)
  }
}
