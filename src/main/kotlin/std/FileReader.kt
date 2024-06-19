package technology.idlab.std

import java.io.File
import technology.idlab.runtime.jvm.Processor
import technology.idlab.runtime.jvm.Writer

class FileReader(args: Map<String, Any>) : Processor(args) {
  /** Arguments */
  private val path: String = this.getArgument("path")
  private val output: Writer = this.getArgument("output")

  /** Read the file as a single byte array and push it down the pipeline. */
  override fun exec() {
    val file = File(path)
    val bytes = file.readBytes()
    output.pushSync(bytes)
    output.close()
  }
}
