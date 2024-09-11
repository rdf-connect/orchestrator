package extensions

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import kotlinx.coroutines.runBlocking
import technology.idlab.extensions.map

class ChannelTest {
  @Test
  fun mapIntToString() = runBlocking {
    val channel: Channel<String> = Channel(Channel.UNLIMITED)

    // Map to the correct types using mapper function.
    val sender: SendChannel<Int> = channel.map(this) { it.toString() }
    val receiver: ReceiveChannel<String> = channel

    sender.send(1)
    sender.send(2)
    sender.send(3)
    sender.close()

    assertEquals("1", receiver.receive())
    assertEquals("2", receiver.receive())
    assertEquals("3", receiver.receive())
  }
}
