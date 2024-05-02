package pipelines

import java.io.File
import org.junit.jupiter.api.Test
import technology.idlab.runner.Pipeline

class RangeReporterTest {
  @Test
  fun exec() {
    val config = this.javaClass.getResource("/pipelines/range_reporter.ttl")
    val file = File(config!!.path)
    val pipeline = Pipeline(file)
    pipeline.executeSync()
  }
}
