package runner.jvm

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import runner.Runner
import technology.idlab.util.Log

class Writer(private val channel: Channel<Runner.Payload>, private val destination: String) {
  fun pushSync(value: ByteArray) = runBlocking { push(value) }

  fun push(value: ByteArray) {
    try {
      channel.trySend(Runner.Payload(destination, value))
    } catch (e: Exception) {
      Log.shared.fatal(e)
    }
  }

  fun close() {
    channel.close()
  }
}
