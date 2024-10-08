package technology.idlab.broker

import technology.idlab.RDFCException

/** An exception was thrown during the execution of a broker. */
abstract class BrokerException : RDFCException() {
  /**
   * An interaction with an unknown channel was requested.
   *
   * @param uri The URI of the channel which is unknown.
   */
  class UnknownChannelException(val uri: String) : BrokerException()

  /**
   * An interaction with a dead channel was requested.
   *
   * @param uri The URI of the channel which is dead.
   */
  class DeadChannelException(val uri: String) : BrokerException()
}
