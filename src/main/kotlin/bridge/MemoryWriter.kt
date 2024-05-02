package technology.idlab.bridge

import bridge.Writer
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import technology.idlab.logging.Log

class MemoryWriter: Writer {
    private var channel: Channel<ByteArray>? = null

    fun setChannel(channel: Channel<ByteArray>) {
        if (this.channel != null) {
            Log.shared.fatal("Channel already set")
        }

        this.channel = channel
    }

    override fun pushSync(value: ByteArray) {
        val channel = this.channel ?: Log.shared.fatal("Channel not set")
        runBlocking { channel.send(value) }
    }

    override suspend fun push(value: ByteArray) {
        val channel = this.channel ?: Log.shared.fatal("Channel not set")

        try {
            channel.trySend(value)
        } catch (e: Exception) {
            Log.shared.fatal(e)
        }
    }

    override fun close() {
        val channel = this.channel ?: Log.shared.fatal("Channel not set")
        channel.close()
    }
}
