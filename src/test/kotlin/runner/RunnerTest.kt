package runner

import kotlin.test.Test
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.util.Log

abstract class RunnerTest {
  abstract val target: String
  abstract val metadata: Map<String, String>

  abstract fun createRunner(): Runner

  @AfterEach fun allowGracefulShutdown() = runBlocking { delay(2000) }

  @Test
  fun loadTest() = runBlocking {
    val runner = createRunner()
    runner.load(createProcessor(), createStage())
    runner.exit()
  }

  @Test
  fun channelTest(): Unit = runBlocking {
    val runner = createRunner()

    // Prepare the runner.
    runner.load(createProcessor(), createStage())

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

  private val paramInput =
      IRParameter(
          simple = IRParameter.Type.READER,
          presence = IRParameter.Presence.REQUIRED,
          count = IRParameter.Count.SINGLE,
      )

  private val paramOutput =
      IRParameter(
          simple = IRParameter.Type.WRITER,
          presence = IRParameter.Presence.REQUIRED,
          count = IRParameter.Count.SINGLE,
      )

  private fun createProcessor(): IRProcessor {
    return IRProcessor(
        "transparent",
        this.target,
        "",
        mapOf(
            "input" to this.paramInput,
            "output" to this.paramOutput,
        ),
        this.metadata,
    )
  }

  private fun createStage(): IRStage {
    return IRStage(
        "transparent_stage",
        this.createProcessor().uri,
        mapOf(
            "input" to IRArgument(simple = listOf("channel_in_uri")),
            "output" to IRArgument(simple = listOf("channel_out_uri"))),
    )
  }
}
