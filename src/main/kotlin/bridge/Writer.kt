package bridge

interface Writer {
    fun pushSync(value: ByteArray)
    fun close()
}
