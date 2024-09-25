package technology.idlab.broker.impl

import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.broker.BrokerException
import technology.idlab.util.Log

typealias Clients<T> = Pair<Int, MutableSet<BrokerClient<T>>>

class SimpleBroker<T>(clients: Collection<BrokerClient<T>>) : Broker<T> {
  /** The collection of readers and writers for each channel. */
  private val clients: MutableMap<String, Clients<T>>

  init {
    val result = mutableMapOf<String, Clients<T>>()

    for (client in clients) {
      client.inject(this)

      for (uri in client.receiving) {
        val (rc, receivers) = result.getOrPut(uri) { Pair(0, mutableSetOf()) }
        receivers.add(client)
        result[uri] = Pair(rc, receivers)
      }

      for (uri in client.sending) {
        val (rc, receivers) = result.getOrPut(uri) { Pair(0, mutableSetOf()) }
        result[uri] = Pair(rc + 1, receivers)
      }
    }

    this.clients = result
  }

  override fun send(uri: String, data: T) {
    Log.shared.debug { "Brokering message: '$uri'" }
    val (_, receivers) = clients[uri] ?: throw BrokerException.UnknownChannelException(uri)

    if (receivers.size == 0) {
      throw BrokerException.DeadChannelException(uri)
    }

    // Send to the clients.
    for (receiver in receivers) {
      receiver.receiveBrokerMessage(uri, data)
    }
  }

  override fun unregister(uri: String) {
    Log.shared.debug { "Unregistering sender from '$uri'" }

    // Decrease the sender count.
    val (rc, receivers) = clients[uri] ?: throw BrokerException.UnknownChannelException(uri)
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
