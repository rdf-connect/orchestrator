package runner.jvm

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import technology.idlab.util.Log

class Writer(private val channel: Channel<ByteArray>) {
  fun pushSync(value: ByteArray) {
    runBlocking { channel.send(value) }
  }

  fun push(value: ByteArray) {
    try {
      channel.trySend(value)
    } catch (e: Exception) {
      Log.shared.fatal(e)
    }
  }

  fun close() {
    channel.close()
  }
}
