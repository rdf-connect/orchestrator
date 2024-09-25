package e2e

import java.io.File
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlinx.coroutines.runBlocking
import technology.idlab.exec
import technology.idlab.install

class E2ETest {
  data class Config(
      val ttl: String,
      val valid: String,
      val report: String,
      val web: String? = null
  )

  private fun run(config: Config) = runBlocking {
    // Reset output files.
    val valid = File(config.valid)
    valid.delete()

    val report = File(config.report)
    report.delete()

    val web =
        if (config.web != null) {
          File(config.web)
        } else {
          null
        }
    web?.delete()

    // Read the pipeline file.
    val pipeline = this::class.java.getResource(config.ttl)
    assertNotNull(pipeline, "The file should exist.")

    // Execute the pipeline.
    install(pipeline.path)
    exec(pipeline.path)

    // Check the output files.
    assert(valid.exists()) { "The valid file should exist." }
    assert(report.exists()) { "The invalid file should exist." }

    assert(valid.readText().isNotEmpty()) { "The valid file should not be empty." }
    assert(report.readText().isNotEmpty()) { "The invalid file should not be empty." }

    assert(valid.readText().contains("<Ghent>"))
    assert(report.readText().contains("sh:Violation"))

    if (web != null) {
      assert(web.exists()) { "The web page should exist." }
      assert(web.readText().contains("<title>Example Domain</title>"))
    }
  }

  @Test
  fun python() {
    val config =
        Config(
            "/e2e/python.ttl",
            "/tmp/rdfc-testing-python-valid.ttl",
            "/tmp/rdfc-testing-python-report.ttl")
    run(config)
  }

  @Test
  fun node() {
    val config =
        Config(
            "/e2e/node.ttl",
            "/tmp/rdfc-testing-node-valid.ttl",
            "/tmp/rdfc-testing-node-report.ttl")
    run(config)
  }

  @Test
  fun jvm() {
    val config =
        Config(
            "/e2e/jvm.ttl",
            "/tmp/rdfc-testing-jvm-valid.ttl",
            "/tmp/rdfc-testing-jvm-report.ttl",
            "/tmp/rdfc-testing-jvm-web.html",
        )
    run(config)
  }
}
