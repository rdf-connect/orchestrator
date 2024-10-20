package technology.idlab.fileutils

import java.io.File
import kotlinx.coroutines.channels.SendChannel
import technology.idlab.rdfc.processor.Arguments
import technology.idlab.rdfc.processor.Processor

/**
 * A simple processor which reads files from the file system and pushes them down the pipeline.
 *
 * @property paths The raw paths to the files to read.
 * @property outgoing The channel to push the file contents down the pipeline.
 */
class FileReader(args: Arguments) : Processor(args) {
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
