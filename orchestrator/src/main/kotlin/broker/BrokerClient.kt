package technology.idlab.broker

/**
 * In order to interact with a broker, an object must implement this interface. It must list all the
 * channels it wants to listen to, and all the channels it wants to send to.
 *
 * @param T The type of the data to receive.
 * @see Broker
 * @see BrokerException
 */
interface BrokerClient<T> {
  /** Reference to the broker itself. This value may be implemented as `lateinit`. */
  var broker: Broker<T>

  /** The URIs the client wants to listen to. */
  val receiving: Collection<String>

  /** The URIs the clients wants to send to. */
  val sending: Collection<String>

  /**
   * Receive a new message from the broker. Note that it is up to the client to handle the message
   * in order, as messages may be delivered before the previous one is handled.
   *
   * @param uri The channel URI of the message.
   * @param data The data received.
   */
  fun receiveBrokerMessage(uri: String, data: T)

  /**
   * The broker calls this method when a channel is closed.
   *
   * @param uri The URI of the closed channel.
   */
  fun closingBrokerChannel(uri: String)

  /**
   * Inject the broker into the client. This method is called by the broker itself and should not be
   * called by the client.
   *
   * @param broker The broker to inject.
   */
  fun inject(broker: Broker<T>) {
    this.broker = broker
  }
}
