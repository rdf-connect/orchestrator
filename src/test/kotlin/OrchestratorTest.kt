import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import processors.NodeTransparent
import processors.TappedReader
import processors.TappedWriter
import technology.idlab.Orchestrator
import technology.idlab.parser.intermediate.IRPipeline

val processors = listOf(TappedWriter.processor, NodeTransparent.processor, TappedReader.processor)

val stages =
    listOf(TappedWriter.stage("in"), NodeTransparent.stage("in", "out"), TappedReader.stage("out"))

val pipeline =
    IRPipeline(
        uri = "pipeline",
        dependencies = emptyList(),
        stages = stages,
    )

class OrchestratorTest {
  @Test
  fun channelTest(): Unit = runBlocking {
    val orchestrator = Orchestrator(pipeline, processors)

    // Bring pipeline online.
    launch { orchestrator.exec() }

    // Send message into the pipeline.
    val data = "Hello, World!".encodeToByteArray()
    TappedWriter.input.send(data)

    // Check the result.
    val result = TappedReader.output.receive()
    assertEquals(data.decodeToString(), result.decodeToString())
  }
}
