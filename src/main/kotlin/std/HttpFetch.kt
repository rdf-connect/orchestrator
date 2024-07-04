package technology.idlab.std

import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import runner.jvm.Processor
import runner.jvm.Writer
import technology.idlab.runner.jvm.Arguments

class HttpFetch(args: Arguments) : Processor(args) {
  /** Meta configuration. */
  private var engine: HttpClientEngine = CIO.create()

  /** Parameters. */
  private val endpoint: String = arguments["endpoint"]
  private val output: Writer = arguments["output"]
  private val headers: Array<String> = arguments["headers"]
  private val method: String = arguments.get<String?>("method") ?: "GET"

  /** Prebuild request. */
  private val builder = HttpRequestBuilder()

  /** Build the HTTP request. */
  init {
    builder.url(endpoint)
    builder.method = HttpMethod.parse(method)
    headers.map { header ->
      val (key, value) = header.split(":").map { it.trim() }
      builder.headers.append(key, value)
    }
  }

  /** Execute an HTTP request and output it to the writer. */
  override suspend fun exec() {
    val client = HttpClient(engine)
    val res = client.request(builder)

    // Check validity of result.
    if (!res.status.isSuccess()) {
      log.fatal("ERROR: Status code ${res.status.value} received from $endpoint")
    }

    // Push the result to the output.
    val bytes = res.readBytes()
    output.push(bytes)
  }

  internal fun overwriteEngine(engine: HttpClientEngine) {
    this.engine = engine
  }
}
