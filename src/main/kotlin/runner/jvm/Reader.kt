package runner.jvm

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.runBlocking
import technology.idlab.util.Log

class Reader(private val channel: Channel<ByteArray>) {
  enum class ResultType {
    SUCCESS,
    CLOSED,
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

  fun readSync(): Result = runBlocking { read() }

  suspend fun read(): Result {
    return try {
      val result = channel.receive()
      Result.success(result)
    } catch (e: ClosedReceiveChannelException) {
      Result.closed()
    } catch (e: Exception) {
      Log.shared.fatal(e)
    }
  }
}
