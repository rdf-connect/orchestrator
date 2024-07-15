package technology.idlab.std

import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor
import technology.idlab.runner.impl.jvm.Reader
import technology.idlab.runner.impl.jvm.Writer

class Transparent(args: Arguments) : Processor(args) {
  private val input: Reader = arguments["input"]
  private val output: Writer = arguments["output"]

  override suspend fun exec() {
    output.push(input.read())
  }
}
