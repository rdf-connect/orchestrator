package technology.idlab.broker.impl

import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.util.Log

typealias Clients<T> = Pair<Int, MutableSet<BrokerClient<T>>>

class SimpleBroker<T> : Broker<T> {
  /** The collection of readers and writers for each channel. */
  private val clients = mutableMapOf<String, Clients<T>>()

  override fun send(uri: String, data: T) {
    Log.shared.debug { "Brokering message: '$uri'" }
    val (_, receivers) = clients[uri] ?: Log.shared.fatal("Channel not registered: $uri")

    // If all readers went offline, we can no longer send any messages to the channel.
    if (receivers.size == 0) {
      Log.shared.fatal("Channel no longer available: $uri")
    }

    // Send to the clients.
    for (receiver in receivers) {
      receiver.receiveBrokerMessage(uri, data)
    }
  }

  override fun registerSender(uri: String) {
    Log.shared.debug { "Registering sender to: '$uri'" }
    val (rc, receivers) = clients.getOrPut(uri) { Pair(0, mutableSetOf()) }
    clients[uri] = Pair(rc + 1, receivers)
  }

  override fun registerReceiver(uri: String, receiver: BrokerClient<T>) {
    Log.shared.debug { "Registering receiver '${receiver.uri}' to '$uri'" }
    val (_, receivers) = clients.getOrPut(uri) { Pair(0, mutableSetOf()) }
    receivers.add(receiver)
  }

  override fun unregisterSender(uri: String) {
    Log.shared.debug { "Unregistering sender from '$uri'" }

    // Decrease the sender count.
    val (rc, receivers) = clients[uri] ?: Log.shared.fatal("Channel not found: $uri")
    clients[uri] = Pair(rc - 1, receivers)

    // If no writers, remain, remove the readers.
    if (rc == 1) {
      Log.shared.debug { "Closing channel: $uri" }
      this.close(uri)
    }
  }

  /**
   * Close a channel by URI.
   *
   * @param uri The URI of the channel that should be closed.
   */
  private fun close(uri: String) {
    Log.shared.debug { "Closing channel: $uri" }
    val (_, receivers) = clients[uri]!!
    receivers.forEach { it.closingBrokerChannel(uri) }
  }
}
