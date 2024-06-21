package runner

import kotlin.concurrent.thread
import kotlin.test.Test
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

abstract class RunnerTest {
  abstract val target: Runner.Target
  abstract val metadata: Map<String, String>

  abstract fun createRunner(): Runner

  @Test
  fun prepareProcessorTest() = runBlocking {
    val runner = createRunner()
    runner.prepare(createProcessor())
  }

  @Test
  fun prepareStageTest() = runBlocking {
    val runner = createRunner()
    runner.prepare(createProcessor())
    runner.prepare(createStage())
  }

  @Test
  fun channelTest(): Unit = runBlocking {
    val runner = createRunner()
    try {
      // Prepare the runner.
      runner.prepare(createProcessor())
      runner.prepare(createStage())

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
      assertEquals(data.decodeToString(), result.data.decodeToString())

      // Halt the runner.
      runner.halt()
      execution.interrupt()
    } catch (_: InterruptedException) {
      // Ignore.
    }
  }

  private fun createProcessor(): IRProcessor {
    return IRProcessor(
        "transparent",
        this.target,
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
        this.metadata,
    )
  }

  private fun createStage(): IRStage {
    return IRStage(
        "transparent_stage",
        this.createProcessor(),
        listOf(
            IRArgument("input", listOf("channel_in_uri")),
            IRArgument("output", listOf("channel_out_uri"))),
    )
  }
}
