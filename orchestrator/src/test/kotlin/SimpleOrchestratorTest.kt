import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import processors.TappedReader
import processors.TappedWriter
import technology.idlab.intermediate.IRRunner
import technology.idlab.orchestrator.impl.SimpleOrchestrator

val stages = listOf(TappedWriter.stage("channel"), TappedReader.stage("channel"))

val jvmRunner =
    IRRunner(
        uri = "https://www.rdf-connect.com/#JVMRunner",
        type = IRRunner.Type.BUILT_IN,
    )

class SimpleOrchestratorTest {
  @Test
  fun channelTest(): Unit = runBlocking {
    val orchestrator = SimpleOrchestrator(stages, listOf(jvmRunner))

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
