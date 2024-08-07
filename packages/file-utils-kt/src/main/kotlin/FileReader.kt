package technology.idlab.fileutils

import java.io.File
import kotlinx.coroutines.channels.SendChannel
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor

class FileReader(args: Arguments) : Processor(args) {
  /** Arguments */
  private val paths: List<String> by args
  private val outgoing: SendChannel<ByteArray> by args

  /** Read the file as a single byte array and push it down the pipeline. */
  override suspend fun exec() {
    for (path in paths) {
      val file = File(path.removePrefix("file:"))
      val bytes = file.readBytes()
      outgoing.send(bytes)
    }

    outgoing.close()
  }
}
