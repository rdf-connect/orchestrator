package technology.idlab.runner.impl.jvm

import kotlinx.coroutines.channels.ReceiveChannel
import technology.idlab.util.Log

class Reader(private val channel: ReceiveChannel<ByteArray>, private val channelURI: String) {
  suspend fun read(): ByteArray {
    val result = channel.receive()
    Log.shared.debug("[$channelURI] -> '${result.decodeToString()}'")
    return result
  }
}
