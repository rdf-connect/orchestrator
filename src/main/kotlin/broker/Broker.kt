package technology.idlab.broker

/**
 * A broker is a simple class which takes in messages targeting a specific URI, and routes them to
 * all registered receivers.
 */
interface Broker<T> {
  /**
   * Send a message to a channel with a given URI.
   *
   * @param uri The URI of the channel.
   * @param data The data to send.
   */
  fun send(uri: String, data: T)

  /**
   * Register a sender to a channel with a given URI.
   *
   * @param uri The URI of the channel.
   */
  fun registerSender(uri: String)

  /**
   * Register a receiver to a channel with a given URI.
   *
   * @param uri The URI of the channel.
   * @param receiver The `BrokerReceiver` to register.
   */
  fun registerReceiver(uri: String, receiver: BrokerClient<T>)

  /**
   * Remove a sender from a channel. If no senders remain, the channel is closed.
   *
   * @param uri The URI to which the sender no longer wants to listen to.
   */
  fun unregisterSender(uri: String)
}
