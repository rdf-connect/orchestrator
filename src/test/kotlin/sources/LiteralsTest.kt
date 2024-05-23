package sources

import java.io.File
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import technology.idlab.runner.Pipeline

class LiteralsTest {
  @Test
  fun parameters() {
    val configPath = this.javaClass.getResource("/pipelines/literals.ttl")
    val config = File(configPath!!.path)
    val pipeline = Pipeline(config)
    val processors = pipeline.processors

    // Get the sole processor in the pipeline.
    assertEquals(processors.size, 1)
    val processor = processors[0]

    // Retrieve arguments.
    val bool: Boolean = processor.getArgument("_bool")
    val byte: Byte = processor.getArgument("_byte")
    val dateTime: Date = processor.getArgument("_dateTime")
    val double: Double = processor.getArgument("_double")
    val float: Float = processor.getArgument("_float")
    val int: Int = processor.getArgument("_int")
    val long: Long = processor.getArgument("_long")
    val string: String = processor.getArgument("_string")

    // Check if results are correct.
    assert(bool)
    assertEquals(24.toByte(), byte)
    assertEquals(1716459630000, dateTime.time)
    assertEquals(3.1415, double)
    assertEquals(3.14f, float)
    assertEquals(242424, int)
    assertEquals(242424242424, long)
    assertEquals("Hello, World!", string)
  }
}
