package technology.idlab.std

import java.io.File
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor
import technology.idlab.runner.impl.jvm.Reader

class FileWriter(args: Arguments) : Processor(args) {
  /** Processor default values. */
  private val overwriteDefault = true
  private val appendDefault = false

  /** Arguments */
  private val path: String = arguments["path"]
  private val file = File(path)
  private val input: Reader = arguments["input"]
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
    while (true) {
      val result = input.read()
      file.appendBytes(result)
    }
  }
}
