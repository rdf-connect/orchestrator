import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import processors.TappedReader
import processors.TappedWriter
import technology.idlab.Orchestrator
import technology.idlab.intermediate.IRRunner

val processors = listOf(TappedWriter.processor, TappedReader.processor)

val stages = listOf(TappedWriter.stage("channel"), TappedReader.stage("channel"))

val jvmRunner =
    IRRunner(
        uri = "https://www.rdf-connect.com/#JVMRunner",
        type = IRRunner.Type.BUILT_IN,
    )

class OrchestratorTest {
  @Test
  fun channelTest(): Unit = runBlocking {
    val orchestrator = Orchestrator(stages, processors, listOf(jvmRunner))

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
