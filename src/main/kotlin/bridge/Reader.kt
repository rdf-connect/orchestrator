package bridge

import technology.idlab.logging.Log


interface Reader {
    enum class ResultType {
        SUCCESS, CLOSED;
    }

    class Result(private val type: ResultType, value: ByteArray) {
        val value: ByteArray
            get() {
                if (type == ResultType.SUCCESS) {
                    return field
                } else {
                    Log.shared.fatal("Cannot get value from invalid read.")
                }
            }

        init {
            this.value = value
        }

        fun isClosed(): Boolean {
            return type == ResultType.CLOSED
        }

        companion object {
            fun success(value: ByteArray): Result {
                return Result(ResultType.SUCCESS, value)
            }

            fun closed(): Result {
                return Result(ResultType.CLOSED, ByteArray(0))
            }
        }
    }

    suspend fun read(): Result
    fun readSync(): Result
    fun isClosed(): Boolean
}

