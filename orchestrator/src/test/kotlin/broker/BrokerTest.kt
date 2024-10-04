package broker

import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.jupiter.api.assertThrows
import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.broker.BrokerException
import technology.idlab.broker.simple.SimpleBroker

/** This abstract class provides some simple tests for concrete broker implementations. */
abstract class BrokerTest {
  /**
   * Helper function for concrete implementations which should return a broker for the given
   * clients.
   *
   * @param clients The clients to register with the broker.
   * @return The broker to use for testing.
   */
  abstract fun setup(clients: Collection<BrokerClient<Int>>): Broker<Int>

  /**
   * Function which creates a simple broker with a sender and receiver.
   *
   * @return A triple containing the sender, receiver, and broker.
   */
  private fun setup(): Triple<BrokerClientMock, BrokerClientMock, Broker<Int>> {
    val sender =
        BrokerClientMock(
            uri = "https://example.com/#sender", sending = listOf("https://example.com/#channel"))

    val receiver =
        BrokerClientMock(
            uri = "https://example.com/#receiver",
            receiving = setOf("https://example.com/#channel"))

    val broker = setup(setOf(sender, receiver))

    return Triple(sender, receiver, broker)
  }

  @Test
  fun unregisterAndClose() {
    val (sender, receiver, broker) = setup()

    // No messages should have been received.
    for (client in listOf(sender, receiver)) {
      assertEquals(emptySet(), client.closedChannels)
      assertEquals(emptyMap(), client.receivedMessages)
    }

    // Unregister the channel.
    broker.unregister("https://example.com/#channel")

    // The receiver should have been notified the channel closed.
    assertEquals(setOf("https://example.com/#channel"), receiver.closedChannels)
    assertEquals(emptyMap(), receiver.receivedMessages)

    // The sender should not have gotten such message.
    assertEquals(setOf(), sender.closedChannels)
    assertEquals(emptyMap(), sender.receivedMessages)
  }

  @Test
  fun deadChannel() {
    val sender =
        BrokerClientMock(
            uri = "https://example.com/#sender", sending = listOf("https://example.com/#channel"))

    val broker = SimpleBroker(setOf(sender))

    assertThrows<BrokerException.DeadChannelException> {
      broker.send("https://example.com/#channel", 0)
    }
  }

  @Test
  fun unknownChannel() {
    val (_, _, broker) = setup()

    assertThrows<BrokerException.UnknownChannelException> {
      broker.send("https://example.com/#unknown", 0)
    }

    assertThrows<BrokerException.UnknownChannelException> {
      broker.unregister("https://example.com/#unknown")
    }
  }

  @Test
  fun sendMessage() {
    val (sender, receiver, broker) = setup()

    // Send a message.
    broker.send("https://example.com/#channel", 1)

    // The receiver should have received the message.
    assertEquals(
        mapOf("https://example.com/#channel" to mutableListOf(1)), receiver.receivedMessages)

    // The sender should not have received the message.
    assertEquals(emptyMap(), sender.receivedMessages)
  }

  @Test
  fun noSender() {
    val receiver =
        BrokerClientMock(
            uri = "https://example.com/#receiver",
            receiving = setOf("https://example.com/#channel"))

    val broker = SimpleBroker(setOf(receiver))

    assertThrows<BrokerException.NoRegisteredSenderException> {
      broker.send("https://example.com/#channel", 0)
    }
  }

  @Test
  fun closeUnknownChannel() {
    val (_, _, broker) = setup()

    assertThrows<BrokerException.UnknownChannelException> {
      broker.unregister("https://example.com/#unknown")
    }
  }
}
