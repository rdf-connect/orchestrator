package technology.idlab.bridge

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import technology.idlab.logging.Log

class MemoryReader : Reader {
  private var channel: Channel<ByteArray>? = null

  fun setChannel(channel: Channel<ByteArray>) {
    if (this.channel != null) {
      Log.shared.fatal("Channel already set")
    }

    this.channel = channel
  }

  override fun readSync(): Reader.Result {
    val channel = this.channel ?: Log.shared.fatal("Channel not set")

    val result = runBlocking { channel.receiveCatching() }

    // Check if the channel got closed.
    if (result.isClosed) {
      return Reader.Result.closed()
    }

    // If an error occurred, the runner must handle it itself.
    if (result.isFailure) {
      Log.shared.fatal("Failed to read bytes")
    }

    val bytes = result.getOrThrow()
    return Reader.Result.success(bytes)
  }

  override suspend fun read(): Reader.Result {
    val channel = this.channel ?: Log.shared.fatal("Channel not set")

    return try {
      val result = channel.receive()
      Reader.Result.success(result)
    } catch (e: ClosedReceiveChannelException) {
      Reader.Result.closed()
    } catch (e: Exception) {
      Log.shared.fatal(e)
    }
  }

  override fun isClosed(): Boolean {
    val channel = this.channel ?: Log.shared.fatal("Channel not set")
    return channel.isClosedForSend
  }
}
