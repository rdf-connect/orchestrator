package technology.idlab.rdfc.orchestrator.broker.exception

import technology.idlab.rdfc.orchestrator.broker.BrokerException

/**
 * An interaction with an unknown channel was requested.
 *
 * @param uri The URI of the channel.
 */
class UnknownChannelException(val uri: String) : BrokerException()
