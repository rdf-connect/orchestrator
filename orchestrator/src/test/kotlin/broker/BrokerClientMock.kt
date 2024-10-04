package broker

import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient

/**
 * A broker client which registers all collects all calls in collections for testing purposes only.
 */
class BrokerClientMock(
    /** The URI of the client. */
    override val uri: String = "https://example.com/#client",
    /** The collections of channels to which the client will listen. */
    override val receiving: Collection<String> = setOf(),
    /** the collection of channels to which the client mauy write. */
    override val sending: Collection<String> = setOf(),
) : BrokerClient<Int> {
  /** The set of closed channels. */
  val closedChannels = mutableSetOf<String>()

  /** All the received messages in the order they arrived at. */
  val receivedMessages = mutableMapOf<String, MutableList<Int>>()

  override lateinit var broker: Broker<Int>

  /*
   * Simply add the URI of the closing channel to the set.
   */
  override fun closingBrokerChannel(uri: String) {
    closedChannels.add(uri)
  }

  /*
   * Add the received messages to the set.
   */
  override fun receiveBrokerMessage(uri: String, data: Int) {
    receivedMessages.getOrPut(uri) { mutableListOf() }.add(data)
  }
}
