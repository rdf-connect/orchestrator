package channels

import bridge.HttpReader
import kotlin.test.Test
import kotlin.test.assertEquals
import technology.idlab.bridge.HttpWriter

class HttpTest {
  @Test
  fun exec() {
    // Create a writer and a reader.
    val writer = HttpWriter("http://localhost:8080")
    val reader = HttpReader()

    // Push a value to the writer and read it from the reader.
    writer.pushSync("Hello, World!".toByteArray())
    val res = reader.readSync()

    // Check if result is the same.
    assertEquals("Hello, World!", String(res.value))
  }
}
