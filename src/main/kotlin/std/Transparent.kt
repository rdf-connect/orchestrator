package technology.idlab.std

import runner.jvm.Processor
import runner.jvm.Reader
import runner.jvm.Writer
import technology.idlab.util.Log

class Transparent(args: Map<String, Any>) : Processor(args) {
  private val input = this.getArgument<Reader>("input")
  private val output = this.getArgument<Writer>("output")

  override fun exec() {
    while (true) {
      val result = input.readSync()

      if (result.isClosed()) {
        break
      }

      Log.shared.info("Received: ${result.value}")
      output.pushSync(result.value)
    }
  }
}
