package technology.idlab.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

internal fun <E, R> Channel<E>.map(scope: CoroutineScope, func: (R) -> E): SendChannel<R> {
  // Create the new channel.
  val result = Channel<R>()

  // Pipe the data through the function and into the new channel.
  scope.launch {
    for (data in result) {
      this@map.send(func(data))
    }
  }

  // Close the new channel if required.
  this.invokeOnClose { result.close() }

  // Return the new channel.
  return result
}
