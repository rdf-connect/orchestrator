package runner.jvm

import kotlin.concurrent.thread
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import runner.Runner
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

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

class JVMRunnerTest {

  private var runner = JVMRunner()

  @BeforeEach
  fun reset() {
    runner = JVMRunner()
  }

  @Test fun prepareProcessorTest() = runBlocking { runner.prepare(processor) }

  @Test
  fun prepareStageTest() = runBlocking {
    runner.prepare(processor)
    runner.prepare(stage)
  }

  @Test
  fun channelTest(): Unit = runBlocking {
    try {
      // Prepare the runner.
      runner.prepare(processor)
      runner.prepare(stage)

      // Execute the runner.
      val execution = thread {
        try {
          runBlocking { runner.exec() }
        } catch (_: InterruptedException) {
          // Ignore.
        }
      }

      // Send message into the pipeline.
      val incoming = runner.getIncomingChannel()
      val data = "Hello, World!".encodeToByteArray()
      incoming.send(Runner.Payload("channel_in_uri", data))

      // Receive message from the pipeline.
      val outgoing = runner.getOutgoingChannel()
      val result = outgoing.receive()
      assertEquals("channel_out_uri", result.destinationURI)
      assertEquals(data, result.data)

      // Halt the runner.
      runner.halt()
      execution.interrupt()
    } catch (_: InterruptedException) {
      // Ignore.
    }
  }
}
