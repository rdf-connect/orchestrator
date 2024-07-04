package technology.idlab.std

import java.io.File
import runner.jvm.Processor
import runner.jvm.Writer
import technology.idlab.runner.jvm.Arguments

class FileReader(args: Arguments) : Processor(args) {
  /** Arguments */
  private val path: String = arguments["path"]
  private val output: Writer = arguments["output"]

  /** Read the file as a single byte array and push it down the pipeline. */
  override suspend fun exec() {
    val file = File(path)
    val bytes = file.readBytes()
    output.push(bytes)
  }
}
