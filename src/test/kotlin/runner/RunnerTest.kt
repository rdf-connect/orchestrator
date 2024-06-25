package runner

import kotlin.test.Test
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

abstract class RunnerTest {
  abstract val target: Runner.Target
  abstract val metadata: Map<String, String>

  abstract fun createRunner(): Runner

  @AfterEach fun allowGracefulShutdown() = runBlocking { delay(2000) }

  @Test
  fun prepareProcessorTest() = runBlocking {
    val runner = createRunner()
    runner.prepare(createProcessor())
    runner.exit()
  }

  @Test
  fun prepareStageTest() = runBlocking {
    val runner = createRunner()
    runner.prepare(createProcessor())
    runner.prepare(createStage())
    runner.exit()
  }

  @Test
  fun channelTest(): Unit = runBlocking {
    val runner = createRunner()

    // Prepare the runner.
    runner.prepare(createProcessor())
    runner.prepare(createStage())

    // Start the runner.
    val job = launch { runner.exec() }

    Log.shared.info("Sending message into the pipeline.")
    val data = "Hello, World!".encodeToByteArray()
    val payload = Runner.Payload("channel_in_uri", data)
    runner.toProcessors.send(payload)

    // Receive message from the pipeline.
    Log.shared.info("Awaiting message from pipeline.")
    val result = runner.fromProcessors.receive()
    assertEquals("channel_out_uri", result.channel)
    assertEquals(data.decodeToString(), result.data.decodeToString())

    job.cancelAndJoin()
    runner.exit()
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
