package technology.idlab.rdfc.core.extensions

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.launch

/**
 * Transform a SendChannel using a mapping function.
 *
 * @param scope The scope in which to run the transformation.
 * @param func The function to apply to each element.
 * @param R The type of the outgoing data after mapping.
 * @param E The type of the outgoing data before mapping.
 * @return A new SendChannel which accepts data of type `R`.
 */
fun <E, R> SendChannel<E>.map(scope: CoroutineScope, func: (R) -> E): SendChannel<R> {
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
