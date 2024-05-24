import technology.idlab.bridge.Reader

class DummyReader(private val contents: Array<ByteArray>) : Reader {
  private var index = 0

  override suspend fun read(): Reader.Result {
    return readSync()
  }

  override fun readSync(): Reader.Result {
    // If we have read all the contents, return closed.
    if (isClosed()) {
      return Reader.Result.closed()
    }

    // Increment the index and return the next value.
    val value = contents[index]
    index += 1
    return Reader.Result.success(value)
  }

  override fun isClosed(): Boolean {
    return index >= contents.size
  }
}
