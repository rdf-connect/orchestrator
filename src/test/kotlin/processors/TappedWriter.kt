package processors

import kotlinx.coroutines.channels.Channel
import runner.Runner
import runner.jvm.Processor
import runner.jvm.Writer
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

/**
 * The TappedWriter processor provides a convenient way to write data into the pipeline during
 * testing. All instances listen to a global channel, which can be used directly during testing to
 * write date to.
 */
class TappedWriter(args: Map<String, Any>) : Processor(args) {
  /** Writer which is exposed to the pipeline. */
  private val output = this.getArgument<Writer>("output")

  /** Continuously read data from the global channel and write it to the output. */
  override suspend fun exec() {
    while (true) {
      output.push(input.receive())
    }
  }

  companion object {
    /** Global channel from which all data is read. */
    val input = Channel<ByteArray>()

    /** Implementation of this processor as IR. */
    private val processor =
        IRProcessor(
            "tapped_writer",
            Runner.Target.JVM,
            mapOf(
                "output" to
                    IRParameter(
                        IRParameter.Type.WRITER,
                        presence = IRParameter.Presence.REQUIRED,
                        count = IRParameter.Count.SINGLE,
                    ),
            ),
            mapOf("class" to "processors.TappedWriter"),
        )

    fun stage(channelURI: String): IRStage {
      return IRStage(
          "tapped_writer_stage", processor, mapOf("output" to IRArgument(listOf(channelURI))))
    }
  }
}
