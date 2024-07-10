package technology.idlab.bridge

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import technology.idlab.logging.Log

class HttpReader(private val port: Int) : Reader {
  private val buffer = Channel<ByteArray>(Channel.Factory.UNLIMITED)

  private val embeddedServer =
      embeddedServer(Netty, port = port) {
            routing {
              post("/") {
                Log.shared.debug("Incoming request")
                val body = call.receive<ByteArray>()
                Log.shared.debug("Received ${body.size} bytes")
                buffer.send(body)

                Log.shared.debug("Responding")
                call.response.status(HttpStatusCode.OK)
                call.respondText("OK")
                Log.shared.debug("Response sent")
              }
            }
          }
          .start(wait = false)

    init {
        Log.shared.debug("Waiting for incoming message on port ${port}")
    }

  override suspend fun read(): Reader.Result {
    try {
      val result = buffer.receive()
      return Reader.Result.success(result)
    } catch (e: ClosedReceiveChannelException) {
      return Reader.Result.closed()
    } catch (e: Exception) {
      Log.shared.fatal(e)
    }
  }

  override fun readSync(): Reader.Result {
    return runBlocking { read() }
  }

  override fun isClosed(): Boolean {
    return false
  }
}
