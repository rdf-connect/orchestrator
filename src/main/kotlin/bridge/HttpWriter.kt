package technology.idlab.bridge

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.runBlocking
import technology.idlab.logging.Log

class HttpWriter(private val endpoint: String) : Writer {
  private val client = HttpClient(CIO)

  override suspend fun push(value: ByteArray) {
    // Create request.
    Log.shared.debug("POST $endpoint (${value.size} bytes)")
    val res = client.post(endpoint) { setBody(value) }
    Log.shared.debug("Received response: ${res.status.value} - ${res.bodyAsText()}")

    // Check status code.
    if (res.status.value != 200) {
      Log.shared.fatal("ERROR: Status code ${res.status.value} received from $endpoint")
    }
  }

  override fun pushSync(value: ByteArray) {
    runBlocking { push(value) }
  }

  override fun close() {
    client.close()
  }
}
