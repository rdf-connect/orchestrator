package technology.idlab.runner.impl.jvm

import kotlinx.coroutines.channels.Channel
import technology.idlab.runner.Runner
import technology.idlab.util.Log

class Writer(private val channel: Channel<Runner.Payload>, private val channelURI: String) {
  suspend fun push(value: ByteArray) {
    Log.shared.debug("'${value.decodeToString()}' -> [$channelURI]")
    channel.send(Runner.Payload(channelURI, value))
  }
}
