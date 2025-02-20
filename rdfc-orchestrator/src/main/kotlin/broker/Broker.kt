package technology.idlab.rdfc.orchestrator.broker

/**
 * A broker is a simple class which takes in messages targeting a specific URI, and routes them to
 * all registered receivers.
 *
 * All receivers must be known during the construction of the broker, but can be removed at any
 * other point in time.
 *
 * If a channel has no remaining senders, it must be closed automatically. The receivers must be
 * notified of this.
 *
 * @param T The type of the data to send.
 * @see BrokerClient
 * @see BrokerException
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
