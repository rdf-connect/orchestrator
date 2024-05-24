package technology.idlab.std

import java.io.File
import technology.idlab.bridge.Reader
import technology.idlab.runner.Processor

class FileWriter(args: Map<String, Any>) : Processor(args) {
  /** Processor default values. */
  private val overwriteDefault = true
  private val appendDefault = false

  /** Arguments */
  private val file = File(this.getArgument<String>("path"))
  private val input: Reader = this.getArgument("input")
  private val overwrite = this.getOptionalArgument<Boolean>("overwrite")
  private val append = this.getOptionalArgument<Boolean>("append")

  init {
    // Sanity check.
    if (overwrite.orElse(false) && append.orElse(false)) {
      log.fatal("Cannot overwrite and append at the same time")
    }

    // Do not overwrite the file if it exists.
    if (file.exists() && !overwrite.orElse(overwriteDefault)) {
      log.fatal("File ${file.path} already exists")
    }

    // Overwrite file if not exists.
    if (file.exists() && !append.orElse(appendDefault)) {
      file.writeBytes(ByteArray(0))
    }
  }

  /** All incoming values are parsed as byte and appended onto the file. */
  override fun exec() {
    while (true) {
      // Read the next incoming value.
      val result = input.readSync()
      if (result.isClosed()) {
        break
      }

      // Append it to the file.
      file.appendBytes(result.value)
    }
  }
}
