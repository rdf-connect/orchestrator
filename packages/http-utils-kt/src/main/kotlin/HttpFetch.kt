package technology.idlab.httputils

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.channels.SendChannel
import technology.idlab.rdfc.processor.Arguments
import technology.idlab.rdfc.processor.Processor

class HttpFetch(args: Arguments) : Processor(args) {
  /** Meta configuration. */
  private var engine: HttpClientEngine = CIO.create()

  /** Default values. */
  private val methodDefault = "GET"

  /** Parameters. */
  private val endpoint: String by args
  private val outgoing: SendChannel<ByteArray> by args
  private val headers: Array<String>? by args
  private val method: String? by args

  /** Prebuild request. */
  private val builder = HttpRequestBuilder()

  /** Build the HTTP request. */
  init {
    builder.url(endpoint)
    builder.method = HttpMethod.parse(method ?: methodDefault)
    headers?.map { header ->
      val (key, value) = header.split(":").map { it.trim() }
      builder.headers.append(key, value)
    }
  }

  /** Execute an HTTP request and output it to the writer. */
  override suspend fun exec() {
    val client = HttpClient(engine)
    val res = client.request(builder)

    // Check validity of result.
    check(res.status.isSuccess()) { "Status code ${res.status.value} received from $endpoint" }

    // Push the result to the output.
    val bytes = res.readBytes()
    outgoing.send(bytes)
    outgoing.close()
  }

  internal fun overwriteEngine(engine: HttpClientEngine) {
    this.engine = engine
  }
}
