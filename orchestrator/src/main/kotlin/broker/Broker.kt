package technology.idlab.broker

/**
 * A broker is a simple class which takes in messages targeting a specific URI, and routes them to
 * all registered receivers.
 *
 * All receivers must be known during the construction of the broker, but can be removed at any
 * other point in time.
 *
 * @param T The type of the data to send.
 */
interface Broker<T> {
  /**
   * Send a message to a channel with a given URI.
   *
   * @param uri The URI of the channel.
   * @param data The data to send.
   * @throws BrokerException
   */
  fun send(uri: String, data: T)

  /**
   * Remove a sender from a channel. If no senders remain, the channel is closed.
   *
   * @param uri The URI to which the sender no longer wants to listen to.
   * @throws BrokerException
   */
  fun unregister(uri: String)
}
