import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.validator.SHACLValidator

class SHACLValidatorTest {
  @Test
  fun test(): Unit = runBlocking {
    val validData = this::class.java.getResourceAsStream("/valid.ttl")!!.readBytes()
    val invalidData = this::class.java.getResourceAsStream("/invalid.ttl")!!.readBytes()

    // Create the validator.
    val incoming = Channel<ByteArray>()
    val outgoing = Channel<ByteArray>()
    val report = Channel<ByteArray>()
    val shapes = this::class.java.getResource("/shapes.ttl")!!.file.toString()
    val arguments =
        Arguments(
            mapOf(
                "incoming" to listOf(incoming),
                "outgoing" to listOf(outgoing),
                "report" to listOf(report),
                "shapes" to listOf(shapes),
                "validation_is_fatal" to listOf(false)))

    val validator = SHACLValidator(arguments)

    // Start execution.
    thread { runBlocking { validator.exec() } }

    // Write valid data.
    incoming.send(validData)

    // Read outgoing data.
    val outgoingData = outgoing.receive()
    assertEquals(validData, outgoingData)

    // Write invalid data.
    incoming.send(invalidData)

    // Read report.
    val reportData = report.receive()
    assert(reportData.isNotEmpty())

    incoming.close()
  }
}
