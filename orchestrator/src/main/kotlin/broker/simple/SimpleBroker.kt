package technology.idlab.broker.simple

import broker.exception.DeadChannelException
import broker.exception.NoRegisteredSenderException
import broker.exception.UnknownChannelException
import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient

/**
 * A simple data class which keeps track of the senders and receivers of a given channel. Note that
 * in the context of the broker, we don't actually care about the sender's URI. We only need to keep
 * track of the **amount** of senders and receivers.
 *
 * @param senderCount The amount of senders for this channel.
 * @param receivers The set of receivers for this channel.
 */
private data class ChannelClients<T>(
    var senderCount: Int = 0,
    val receivers: MutableSet<BrokerClient<T>> = mutableSetOf(),
)

/**
 * A rudimentary implementation which has limited correctness checks and delivers messages as soon
 * as they arrive with no regard to the state of the client.
 *
 * All messages are delivered to the individual clients sequentially, without suspending. This means
 * that if a client is slow to process a message, it will block the delivery of messages to other
 * clients.
 *
 * @see Broker
 */
class SimpleBroker<T>(clients: Collection<BrokerClient<T>>) : Broker<T> {
  /** A map which holds ChannelClients per channel. */
  private val clientsByChannel: Map<String, ChannelClients<T>>

  /*
   * Go over all clients and register them with the broker. Update the clientsByChannel map
   * accordingly.
   */
  init {
    val clientsByChannel = mutableMapOf<String, ChannelClients<T>>()

    for (client in clients) {
      client.inject(this)

      for (uri in client.receiving) {
        clientsByChannel.getOrPut(uri) { ChannelClients() }.receivers.add(client)
      }

      for (uri in client.sending) {
        clientsByChannel.getOrPut(uri) { ChannelClients() }.senderCount += 1
      }
    }

    this.clientsByChannel = clientsByChannel
  }

  override fun send(uri: String, data: T) {
    val channelClients = clientsByChannel[uri] ?: throw UnknownChannelException(uri)

    // Check if there are any active listeners.
    if (channelClients.receivers.size == 0) {
      throw DeadChannelException(uri)
    }

    // Check if there is actually a sender remaining.
    if (channelClients.senderCount == 0) {
      throw NoRegisteredSenderException(uri)
    }

    // Send to the clients.
    for (receiver in channelClients.receivers) {
      receiver.receiveBrokerMessage(uri, data)
    }
  }

  override fun unregister(uri: String) {
    val channelClients = clientsByChannel[uri] ?: throw UnknownChannelException(uri)

    // Check if there is actually a sender remaining.
    if (channelClients.senderCount == 0) {
      throw NoRegisteredSenderException(uri)
    }

    // Decrease the sender count.
    channelClients.senderCount -= 1

    // Check if there are active senders left.
    if (channelClients.senderCount == 0) {
      this.close(uri)
    }
  }

  /**
   * Close a channel and notify all receivers.
   *
   * Note that the receivers are notified sequentially, without suspending. This means that if a
   * receiver is slow to process the closing message, it will block the delivery of close messages
   * to the other receivers.
   *
   * @param uri The URI of the channel.
   * @throws UnknownChannelException If the URI doesn't correspond to a channel.
   */
  private fun close(uri: String) {
    val channelClients = clientsByChannel[uri] ?: throw UnknownChannelException(uri)

    // Close each client separately.
    for (receiver in channelClients.receivers) {
      receiver.closingBrokerChannel(uri)
    }
  }
}
