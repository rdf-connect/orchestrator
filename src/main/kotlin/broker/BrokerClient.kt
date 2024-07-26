package technology.idlab.broker

/**
 * The `BrokerReceiver` may receive messages from the broker which are targeted to specific URIs.
 */
interface BrokerClient<T> {
  /** The URI of the receiver. */
  val uri: String

  /** Reference to the broker itself. */
  var broker: Broker<T>

  /** The URIs the client wants to listen to. */
  val receiving: Set<String>

  /** The URIs the clients wants to send to. */
  val sending: List<String>

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

  /** Dependency injection. */
  fun inject(broker: Broker<T>) {
    this.broker = broker
  }
}
