package technology.idlab.broker

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import technology.idlab.util.Log

enum class Mode {
  READ,
  WRITE,
  READ_WRITE,
}

class Broker<T> {
  /** This job manages the coroutine scope of the broker. */
  private val job = Job()

  /** This scope contains all handlers of asynchronous incoming messages. */
  private val scope = CoroutineScope(job)

  /** The collection of readers and writers for each channel. */
  private val clients =
      mutableMapOf<String, Pair<MutableSet<BrokerClient<T>>, MutableSet<BrokerClient<T>>>>()

  /**
   * Send a message to a channel with a given URI.
   *
   * @param uri The URI of the channel.
   * @param data The data to send.
   */
  fun send(uri: String, data: T) =
      this.scope.launch {
        Log.shared.debug { "Brokering message: '$uri'" }
        val (readers, _) = clients[uri] ?: Log.shared.fatal("Channel not registered: $uri")

        // If all readers went offline, we can no longer send any messages to the channel.
        if (readers.size == 0) {
          Log.shared.fatal("Channel no longer available: $uri")
        }

        // Send to the clients.
        readers.forEach { it.receive(uri, data) }
      }

  /**
   * Register a client to a channel with a given URI.
   *
   * @param uri The URI of the channel.
   * @param client The client to register.
   */
  fun register(uri: String, client: BrokerClient<T>, mode: Mode) {
    Log.shared.debug { "Registering client '${client.uri}' to '$uri'" }
    val (readers, writers) = clients.getOrPut(uri) { Pair(mutableSetOf(), mutableSetOf()) }

    if (mode == Mode.READ || mode == Mode.READ_WRITE) {
      readers.add(client)
    }

    if (mode == Mode.WRITE || mode == Mode.READ_WRITE) {
      writers.add(client)
    }
  }

  /**
   * Remove a client from the brokering service, closing the channel if no other clients are
   * present.
   */
  fun unregister(uri: String, client: BrokerClient<T>) =
      scope.launch {
        Log.shared.debug { "Unregistering client '${client.uri}' to '$uri'" }

        val (readers, writers) = clients[uri] ?: Log.shared.fatal("Channel not found: $uri")

        // Remove from writers.
        writers.remove(client)

        // If no writers, remain, remove the readers.
        if (writers.size == 0) {
          Log.shared.debug { "Closing channel: $uri" }
          readers.forEach { it.close(uri) }
        }
      }
}
