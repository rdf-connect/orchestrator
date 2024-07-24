package technology.idlab.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

internal fun <E, R> SendChannel<E>.map(scope: CoroutineScope, func: (R) -> E): SendChannel<R> {
  // Create the new channel.
  val result = Channel<R>()

  // Pipe the data through the function and into the new channel.
  scope.launch {
    for (data in result) {
      this@map.send(func(data))
    }

    // If the new channel is closed, close the old one.
    this@map.close()
  }

  // Return the new channel.
  return result
}
