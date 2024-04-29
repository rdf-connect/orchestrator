import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import bridge.Bridge
import bridge.Reader
import technology.idlab.logging.Log

class MemoryBridge : Bridge {
    private var channel = Channel<ByteArray>(10)

    override fun pushSync(value: ByteArray) {
        Log.shared.debug("Pushing ${value.size} bytes")
        runBlocking { channel.send(value) }
        Log.shared.debug("Done")
    }

    override fun readSync(): Reader.Result {
        Log.shared.debug("Reading bytes")
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

    override fun isClosed(): Boolean {
        return channel.isClosedForSend
    }

    override fun close() {
        channel.close()
    }
}
