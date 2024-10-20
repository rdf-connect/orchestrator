package technology.idlab.rdfc.core.util

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

/**
 * Retry the execution of a block of code a given number of times. If the block of code throws an
 * exception, the function will wait for a given number of milliseconds before trying again. If the
 * block of code does not succeed after the given number of retries, the function will throw a fatal
 * error.
 */
suspend fun <T> retries(times: Int, milliseconds: Long = 1000, block: suspend () -> T): T =
    coroutineScope {
      var exception: Exception? = null

      for (unused in 0 until times) {
        try {
          return@coroutineScope block()
        } catch (e: Exception) {
          exception = e
          delay(milliseconds)
        }
      }

      throw exception!!
    }
