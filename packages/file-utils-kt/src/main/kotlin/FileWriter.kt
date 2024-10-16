package technology.idlab.fileutils

import java.io.File
import kotlinx.coroutines.channels.ReceiveChannel
import technology.idlab.RDFCException
import technology.idlab.runner.jvm.Arguments
import technology.idlab.runner.jvm.KotlinProcessor

/** A simple processor which writes all incoming data to a file. */
class FileWriter(args: Arguments) : KotlinProcessor(args) {
  /** Processor default values. */
  private val overwriteDefault = true
  private val appendDefault = false

  /** Arguments */
  private val path: String by args
  private val file = File(path.removePrefix("file:"))
  private val incoming: ReceiveChannel<ByteArray> by args
  private val overwrite: Boolean? by args
  private val append: Boolean? by args

  init {
    // Sanity check.
    if (overwrite == true && append == true) {
      throw object : RDFCException() {
        override val message = "Cannot overwrite and append to file at the same time."
      }
    }

    // Do not overwrite the file if it exists.
    if (file.exists() && !(overwrite ?: overwriteDefault)) {
      throw object : RDFCException() {
        override val message = "File ${file.path} already exists."
      }
    }

    // Overwrite file if not exists.
    if (file.exists() && !(append ?: appendDefault)) {
      file.writeBytes(ByteArray(0))
    }
  }

  /** All incoming values are parsed as byte and appended onto the file. */
  override suspend fun exec() {
    for (data in incoming) {
      file.appendBytes(data)
    }
  }
}
