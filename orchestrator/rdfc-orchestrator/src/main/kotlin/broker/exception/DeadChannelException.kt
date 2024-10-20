package technology.idlab.broker.exception

import technology.idlab.broker.BrokerException

/**
 * An interaction with a dead channel was requested.
 *
 * @param uri The URI of the channel.
 */
class DeadChannelException(val uri: String) : BrokerException()
