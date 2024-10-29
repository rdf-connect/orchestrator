package technology.idlab.rdfc.orchestrator.broker.simple

import technology.idlab.rdfc.orchestrator.broker.Broker
import technology.idlab.rdfc.orchestrator.broker.BrokerClient
import technology.idlab.rdfc.orchestrator.broker.BrokerTest

class SimpleBrokerTest : BrokerTest() {
  override fun setup(clients: Collection<BrokerClient<Int>>): Broker<Int> {
    return SimpleBroker(clients)
  }
}
