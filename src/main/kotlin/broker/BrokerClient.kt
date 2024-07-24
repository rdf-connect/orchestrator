package technology.idlab.broker

interface BrokerClient<T> {
  val uri: String

  suspend fun receive(uri: String, data: T)

  suspend fun close(uri: String)
}
