package e2e

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.incremental.createDirectory
import technology.idlab.exec

class E2ETest {
  @Test
  fun node() {
    // Create the output directory.
    val directory = File("/tmp/rdfc-testing")
    directory.createDirectory()

    // Reset output files.
    val valid = File("/tmp/rdfc-testing/valid.ttl")
    valid.delete()
    val report = File("/tmp/rdfc-testing/report.ttl")
    report.delete()

    // Read the pipeline file.
    val pipeline = this::class.java.getResource("/e2e/node.ttl")
    assertNotNull(pipeline, "The file should exist.")

    // Execute the pipeline.
    runBlocking {
      try {
        withTimeout(20_000) { exec(pipeline.path) }
      } catch (_: TimeoutCancellationException) {}
    }

    // Check the output files.
    assert(valid.exists()) { "The valid file should exist." }
    assert(report.exists()) { "The invalid file should exist." }

    assert(valid.readText().isNotEmpty()) { "The valid file should not be empty." }
    assert(report.readText().isNotEmpty()) { "The invalid file should not be empty." }

    assert(valid.readText().contains("<Ghent>"))
    assert(report.readText().contains("sh:focusNode <Barcelona>"))
  }
}
