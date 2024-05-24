package technology.idlab.bridge

interface Writer {
  suspend fun push(value: ByteArray)

  fun pushSync(value: ByteArray)

  fun close()
}
