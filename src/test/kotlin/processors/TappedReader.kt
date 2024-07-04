package processors

import kotlinx.coroutines.channels.Channel
import runner.Runner
import runner.jvm.Processor
import runner.jvm.Reader
import technology.idlab.parser.intermediate.IRArgument
import technology.idlab.parser.intermediate.IRParameter
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.runner.jvm.Arguments

/**
 * The TappedReader processor provides a convenient way to read data from the pipeline during
 * testing. All incoming data will be written to a global channel, which can be used directly during
 * testing to read data from.
 */
class TappedReader(args: Arguments) : Processor(args) {
  /** The channel which is exposed to the pipeline. */
  private val input: Reader = arguments["input"]

  /** Continuously read data from the input and write it to the global channel. */
  override suspend fun exec() {
    while (true) {
      output.send(input.read())
    }
  }

  companion object {
    /** Global channel into which all data is dumped. */
    val output = Channel<ByteArray>()

    /** Implementation of this processor as IR. */
    private val processor =
        IRProcessor(
            "tapped_reader",
            Runner.Target.JVM,
            mapOf(
                "input" to
                    IRParameter(
                        IRParameter.Type.READER,
                        presence = IRParameter.Presence.REQUIRED,
                        count = IRParameter.Count.SINGLE,
                    ),
            ),
            mapOf("class" to "processors.TappedReader"),
        )

    fun stage(channelURI: String): IRStage {
      return IRStage(
          "tapped_reader_stage", processor, mapOf("input" to IRArgument(listOf(channelURI))))
    }
  }
}
