package std

import bridge.DummyWriter
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlin.test.Test
import kotlin.test.assertEquals
import technology.idlab.std.HttpFetch

class HttpFetchTest {
  @Test
  fun functionality() {
    // Initialize the processor arguments.
    val writer = DummyWriter()
    val args =
        mapOf(
            "endpoint" to "http://localhost:8080",
            "method" to "DELETE",
            "output" to writer,
            "headers" to
                arrayOf(
                    "Content-Type: text/plain",
                    "Key: Value",
                ),
        )

    // Mock the HTTP engine.
    val engine = MockEngine { request ->
      // Check URL.
      assertEquals("http://localhost:8080", request.url.toString())

      // Check method.
      assertEquals(HttpMethod.Delete, request.method)

      // Check headers.
      assertEquals("text/plain", request.headers["Content-Type"])
      assertEquals("Value", request.headers["Key"])

      // Send response.
      respond(
          content = ByteReadChannel("Hello, World!"),
          status = HttpStatusCode.OK,
      )
    }

    // Initialize the processor.
    val httpFetch = HttpFetch(args)
    httpFetch.overwriteEngine(engine)

    // Execute.
    httpFetch.exec()
    val results = writer.getValues()

    // Check body of response.
    assertEquals(1, results.size)
    assertEquals("Hello, World!", String(results[0]))
  }
}
