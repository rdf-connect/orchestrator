package technology.idlab.std

import java.io.File
import kotlinx.coroutines.channels.SendChannel
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor

class FileReader(args: Arguments) : Processor(args) {
  /** Arguments */
  private val path: String = arguments["path"]
  private val output: SendChannel<ByteArray> = arguments["output"]

  /** Read the file as a single byte array and push it down the pipeline. */
  override suspend fun exec() {
    val file = File(path)
    val bytes = file.readBytes()
    output.send(bytes)
  }
}
