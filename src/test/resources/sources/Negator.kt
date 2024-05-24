import bridge.Reader
import bridge.Writer
import technology.idlab.runner.Processor

class Negator(args: Map<String, Any>) : Processor(args) {
  private val input: Reader = this.getArgument("input")
  private val output: Writer = this.getArgument("output")

  override fun exec() {
    while (true) {
      // Read the next incoming value.
      val result = input.readSync()
      if (result.isClosed()) {
        break
      }

      // Parse as integer and negate.
      val value = result.value.decodeToString().toInt()
      val next = value * -1

      // Push down the pipeline.
      output.pushSync(next.toString().encodeToByteArray())
    }

    // Propagate the end of the stream.
    output.close()
  }
}
