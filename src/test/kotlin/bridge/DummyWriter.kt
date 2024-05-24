package bridge

class DummyWriter : Writer {
  private val values = mutableListOf<ByteArray>()
  private var closed = false

  override suspend fun push(value: ByteArray) {
    pushSync(value)
  }

  override fun pushSync(value: ByteArray) {
    if (closed) {
      throw IllegalStateException("Cannot push to closed writer.")
    }
    values.add(value)
  }

  override fun close() {
    closed = true
  }

  fun getValues(): List<ByteArray> {
    return values
  }
}
