package broker.simple

import broker.BrokerTest
import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.broker.simple.SimpleBroker

class SimpleBrokerTest : BrokerTest() {
  override fun setup(clients: Collection<BrokerClient<Int>>): Broker<Int> {
    return SimpleBroker(clients)
  }
}
