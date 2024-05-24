package pipelines

import java.io.File
import org.junit.jupiter.api.Test
import technology.idlab.runner.Pipeline

class NegativeRangeTest {
  @Test
  fun exec() {
    val config = this.javaClass.getResource("/pipelines/negative_range.ttl")
    val file = File(config!!.path)
    val pipeline = Pipeline(file)
    pipeline.executeSync()
  }
}
