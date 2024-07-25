package technology.idlab.broker

/**
 * The `BrokerReceiver` may receive messages from the broker which are targeted to specific URIs.
 */
interface BrokerClient<T> {
  /** The URI of the receiver. */
  val uri: String

  /**
   * Receive a new message from the broker. Note that this is a suspending, and should only return
   * once the message has been dealt with, in order to prevent race conditions and out-of-order
   * messages, including the close signal.
   *
   * @param uri The channel URI of the message.
   * @param data The data received.
   */
  fun receiveBrokerMessage(uri: String, data: T)

  /** Close this instance as a receiver for a specific URI. */
  fun closingBrokerChannel(uri: String)
}
