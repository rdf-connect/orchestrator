package technology.idlab.std

import runner.jvm.Processor
import runner.jvm.Reader
import runner.jvm.Writer

class Transparent(args: Map<String, Any>) : Processor(args) {
  private val input = this.getArgument<Reader>("input")
  private val output = this.getArgument<Writer>("output")

  override suspend fun exec() {
    output.push(input.read())
  }
}
