package runner.jvm

import kotlinx.coroutines.channels.Channel
import runner.Runner.Payload
import technology.idlab.util.Log

class Writer(private val channel: Channel<Payload>, private val channelURI: String) {
  suspend fun push(value: ByteArray) {
    Log.shared.debug("'${value.decodeToString()}' -> [$channelURI]")
    channel.send(Payload(channelURI, value))
  }
}
