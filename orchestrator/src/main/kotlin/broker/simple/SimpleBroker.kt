package technology.idlab.broker.simple

import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.broker.BrokerException

/**
 * A simple data class which keeps track of the senders and receivers of a given channel. Note that
 * in the context of the broker, we don't actually care about the sender's URI. We only need to keep
 * track of the amount of senders and receivers.
 */
private data class ChannelClients<T>(
    var senderCount: Int = 0,
    val receivers: MutableSet<BrokerClient<T>> = mutableSetOf(),
)

/**
 * A simple broker implementation which routes messages to all registered receivers. It keeps track
 * of the clients using a simple string-to-ChannelClients map.
 */
class SimpleBroker<T>(clients: Collection<BrokerClient<T>>) : Broker<T> {

  /** The collection of readers and writers for each channel. */
  private val channels: MutableMap<String, ChannelClients<T>>

  init {
    val result = mutableMapOf<String, ChannelClients<T>>()

    for (client in clients) {
      client.inject(this)

      for (uri in client.receiving) {
        result.getOrPut(uri) { ChannelClients() }.receivers.add(client)
      }

      for (uri in client.sending) {
        result.getOrPut(uri) { ChannelClients() }.senderCount += 1
      }
    }

    this.channels = result
  }

  override fun send(uri: String, data: T) {
    val channelClients = channels[uri] ?: throw BrokerException.UnknownChannelException(uri)

    // Check if there are any active listeners.
    if (channelClients.receivers.size == 0) {
      throw BrokerException.DeadChannelException(uri)
    }

    // Check if there is actually a sender remaining.
    if (channelClients.senderCount == 0) {
      throw BrokerException.NoRegisteredSenderException(uri)
    }

    // Send to the clients.
    for (receiver in channelClients.receivers) {
      receiver.receiveBrokerMessage(uri, data)
    }
  }

  override fun unregister(uri: String) {
    // Decrease the sender count.
    val channelClients = channels[uri] ?: throw BrokerException.UnknownChannelException(uri)

    // Check if there is actually a sender remaining.
    if (channelClients.senderCount == 0) {
      throw BrokerException.NoRegisteredSenderException(uri)
    }

    // Decrease the sender count.
    channelClients.senderCount -= 1

    // Check if there are active senders left.
    if (channelClients.senderCount == 0) {
      this.close(uri)
    }
  }

  /**
   * Close a channel by URI.
   *
   * @param uri The URI of the channel that should be closed.
   * @throws BrokerException.UnknownChannelException If the URI doesn't correspond to a channel.
   */
  private fun close(uri: String) {
    val channelClients = channels[uri] ?: throw BrokerException.UnknownChannelException(uri)

    // Close each client separately.
    for (receiver in channelClients.receivers) {
      receiver.closingBrokerChannel(uri)
    }
  }
}
