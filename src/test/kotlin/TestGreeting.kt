import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import technology.idlab.Configuration
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertTrue

class TestGreeting {
    private val standardOut = System.out
    private val outputStreamCaptor = ByteArrayOutputStream()

    @BeforeEach
    fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        System.setOut(standardOut)
    }

    @Test
    fun testGreeting() {
        val config = Configuration("/Users/jens/Developer/technology.idlab.jvm-runner/src/test/resources/Greeting.ttl")
        config.executeAll()
        assertTrue(outputStreamCaptor.toString().contains("Hello, JVM Runner!"))
    }
}
