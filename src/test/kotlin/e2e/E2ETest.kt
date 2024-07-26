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
  private fun run(resource: String) {
    // Create the output directory.
    val directory = File("/tmp/rdfc-testing")
    directory.createDirectory()

    // Reset output files.
    val valid = File("/tmp/rdfc-testing/valid.ttl")
    valid.delete()
    val report = File("/tmp/rdfc-testing/report.ttl")
    report.delete()

    // Read the pipeline file.
    val pipeline = this::class.java.getResource(resource)
    assertNotNull(pipeline, "The file should exist.")

    // Execute the pipeline.
    runBlocking {
      try {
        withTimeout(30_000) { exec(pipeline.path) }
      } catch (_: TimeoutCancellationException) {}
    }

    // Check the output files.
    assert(valid.exists()) { "The valid file should exist." }
    assert(report.exists()) { "The invalid file should exist." }

    assert(valid.readText().isNotEmpty()) { "The valid file should not be empty." }
    assert(report.readText().isNotEmpty()) { "The invalid file should not be empty." }

    assert(valid.readText().contains("<Ghent>"))
    assert(report.readText().contains("sh:conforms"))
  }

  @Test
  fun node() {
    run("/e2e/node.ttl")
  }

  @Test
  fun jvm() {
    // Remove the web page if it already exists.
    val webPage = File("/tmp/rdfc-testing/web.html")
    webPage.delete()

    run("/e2e/jvm.ttl")

    // Check if web page has been fetched and written to disk.
    assert(webPage.readText().contains("<title>Example Domain</title>"))
  }
}
