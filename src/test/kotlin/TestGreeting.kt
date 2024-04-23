import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import technology.idlab.runner.Pipeline
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertTrue

class TestGreeting {
    private val outputStreamCaptor = ByteArrayOutputStream()

    @BeforeEach
    fun setUp() {
        System.setOut(PrintStream(outputStreamCaptor))
    }

    @AfterEach
    fun tearDown() {
        print(outputStreamCaptor.toString())
        System.setOut(System.out)
    }

    @Test
    fun testGreeting() {
        val path = TestGreeting::class.java.getResource("/Greeting.ttl")?.path
        val file = File(path!!)
        val config = Pipeline(file)
        config.executeSync()
        assertTrue(outputStreamCaptor.toString().contains("Hello, JVM Runner!"))
    }
}
