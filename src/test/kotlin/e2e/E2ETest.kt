package e2e

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.jetbrains.kotlin.incremental.createDirectory
import technology.idlab.exec

class E2ETest {
  @Test
  fun node() {
    val pipeline = this::class.java.getResource("/e2e/node.ttl")
    assertNotNull(pipeline, "The file should exist.")

    val directory = File("/tmp/rdfc-testing")
    directory.createDirectory()

    val input = File("/tmp/rdfc-testing/input.txt")
    input.createNewFile()
    input.writeText("Hello, World!")

    val output = File("/tmp/rdfc-testing/output.txt")
    output.delete()
    output.createNewFile()

    runBlocking {
      try {
        withTimeout(10000) { exec(pipeline.path) }
      } catch (_: TimeoutCancellationException) {}
    }

    assertEquals("Hello, World!", output.readText())
  }
}
