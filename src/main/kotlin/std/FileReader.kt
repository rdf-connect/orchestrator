package technology.idlab.std

import java.io.File
import runner.jvm.Processor
import runner.jvm.Writer

class FileReader(args: Map<String, Any>) : Processor(args) {
  /** Arguments */
  private val path: String = this.getArgument("path")
  private val output: Writer = this.getArgument("output")

  /** Read the file as a single byte array and push it down the pipeline. */
  override suspend fun exec() {
    val file = File(path)
    val bytes = file.readBytes()
    output.push(bytes)
  }
}
