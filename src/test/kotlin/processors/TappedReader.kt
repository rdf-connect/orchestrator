package processors

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor

/**
 * The TappedReader processor provides a convenient way to read data from the pipeline during
 * testing. All incoming data will be written to a global channel, which can be used directly during
 * testing to read data from.
 */
class TappedReader(args: Arguments) : Processor(args) {
  /** The channel which is exposed to the pipeline. */
  private val input: ReceiveChannel<ByteArray> = arguments["input"]

  /** Continuously read data from the input and write it to the global channel. */
  override suspend fun exec() {
    output.send(input.receive())
    output.close()
  }

  companion object {
    /** Global channel into which all data is dumped. */
    val output = Channel<ByteArray>()

    /** Implementation of this processor as IR. */
    val processor =
        IRProcessor(
            "tapped_reader",
            "https://www.rdf-connect.com/#JVMRunner",
            null,
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
