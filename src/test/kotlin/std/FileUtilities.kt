package std

import java.io.File
import java.util.*
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.channels.Channel
import technology.idlab.bridge.MemoryReader
import technology.idlab.bridge.MemoryWriter
import technology.idlab.std.FileReader
import technology.idlab.std.FileWriter

class FileUtilities {
  @Test
  fun e2e() {
    val input = File.createTempFile("input", "txt")
    val output = File.createTempFile("output", "txt")

    // Write to the input file.
    input.writeText("Hello, World!")

    // Configure the FileReader processor.
    val channel = Channel<ByteArray>(1)
    val reader = MemoryReader()
    val writer = MemoryWriter()

    reader.setChannel(channel)
    writer.setChannel(channel)

    val fileReader =
        FileReader(
            mapOf(
                "path" to input.path,
                "output" to writer,
            ))

    val fileWriter =
        FileWriter(
            mapOf(
                "path" to output.path,
                "input" to reader,
                "overwrite" to Optional.of(true),
                "append" to Optional.of(false),
            ))

    // Execute the FileReader processor.
    listOf(fileReader, fileWriter).map { thread { it.exec() } }.forEach { it.join() }

    // Check if the output is correct.
    val result = output.readText()
    assertEquals("Hello, World!", result)
  }
}
