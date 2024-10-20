package technology.idlab.broker.exception

import technology.idlab.broker.BrokerException

/**
 * An attempt was made to send a message into a channel where there are no known senders remaining.
 *
 * @param uri The URI of the channel.
 */
class NoRegisteredSenderException(val uri: String) : BrokerException()
