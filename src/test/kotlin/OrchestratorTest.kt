import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import processors.NodeTransparent
import processors.TappedReader
import processors.TappedWriter
import technology.idlab.Orchestrator

class OrchestratorTest {
  @Test
  fun channelTest(): Unit = runBlocking {
    val stages =
        setOf(
            TappedWriter.stage("in"), NodeTransparent.stage("in", "out"), TappedReader.stage("out"))
    val orchestrator = Orchestrator(stages)

    // Bring pipeline online.
    orchestrator.exec()

    // Send message into the pipeline.
    val data = "Hello, World!".encodeToByteArray()
    TappedWriter.input.send(data)

    // Check the result.
    val result = TappedReader.output.receive()
    assertEquals(data.decodeToString(), result.decodeToString())
  }
}
