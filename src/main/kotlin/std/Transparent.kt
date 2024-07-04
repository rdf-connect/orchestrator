package technology.idlab.std

import runner.jvm.Processor
import runner.jvm.Reader
import runner.jvm.Writer
import technology.idlab.runner.jvm.Arguments

class Transparent(args: Arguments) : Processor(args) {
  private val input: Reader = arguments["input"]
  private val output: Writer = arguments["output"]

  override suspend fun exec() {
    output.push(input.read())
  }
}
