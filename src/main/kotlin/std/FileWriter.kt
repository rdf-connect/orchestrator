package technology.idlab.std

import java.io.File
import kotlinx.coroutines.channels.ReceiveChannel
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor

class FileWriter(args: Arguments) : Processor(args) {
  /** Processor default values. */
  private val overwriteDefault = true
  private val appendDefault = false

  /** Arguments */
  private val path: String = arguments["path"]
  private val file = File(path)
  private val input: ReceiveChannel<ByteArray> = arguments["input"]
  private val overwrite: Boolean? = arguments["overwrite"]
  private val append: Boolean? = arguments["append"]

  init {
    // Sanity check.
    if (overwrite == true && append == true) {
      log.fatal("Cannot overwrite and append at the same time")
    }

    // Do not overwrite the file if it exists.
    if (file.exists() && !(overwrite ?: overwriteDefault)) {
      log.fatal("File ${file.path} already exists")
    }

    // Overwrite file if not exists.
    if (file.exists() && !(append ?: appendDefault)) {
      file.writeBytes(ByteArray(0))
    }
  }

  /** All incoming values are parsed as byte and appended onto the file. */
  override suspend fun exec() {
    for (data in input) {
      file.appendBytes(data)
    }
  }
}
