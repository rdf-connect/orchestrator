package runner.jvm

import kotlin.test.Test
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import runner.Runner
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

class JVMRunnerTest {
  private val processor =
      IRProcessor(
          "transparent",
          Runner.Target.JVM,
          listOf(
              IRParameter(
                  "input",
                  IRParameter.Type.READER,
                  IRParameter.Presence.REQUIRED,
                  IRParameter.Count.SINGLE,
              ),
              IRParameter(
                  "output",
                  IRParameter.Type.WRITER,
                  IRParameter.Presence.REQUIRED,
                  IRParameter.Count.SINGLE,
              ),
          ),
          mapOf("class" to "technology.idlab.std.Transparent"))

  private val stage =
      IRStage(
          "transparent_stage",
          processor,
          listOf(
              IRArgument("input", listOf("channel_in_uri")),
              IRArgument("output", listOf("channel_out_uri"))),
      )

  private var incoming: Channel<Runner.Payload> = Channel()
  private var outgoing: Channel<Runner.Payload> = Channel()
  private var runner = JVMRunner(incoming, outgoing)

  @BeforeEach
  fun reset() {
    incoming = Channel()
    outgoing = Channel()
    runner = JVMRunner(incoming, outgoing)
  }

  @Test fun prepareProcessorTest() = runBlocking { runner.prepare(processor) }

  @Test
  fun prepareStageTest() = runBlocking {
    runner.prepare(processor)
    runner.prepare(stage)
  }

  @Test
  fun channelTest() = runBlocking {
    runner.prepare(processor)
    runner.prepare(stage)
    val execution = async { runner.exec() }

    val data = "Hello, World!".encodeToByteArray()
    incoming.send(Runner.Payload("channel_in_uri", data))
    val result = outgoing.receive()
    assertEquals("channel_out_uri", result.destinationURI)
    assertEquals(data, result.data)
  }
}
