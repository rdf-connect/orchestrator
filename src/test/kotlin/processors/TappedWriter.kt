package processors

import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.SendChannel
import technology.idlab.intermediate.IRArgument
import technology.idlab.intermediate.IRParameter
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor

/**
 * The TappedWriter processor provides a convenient way to write data into the pipeline during
 * testing. All instances listen to a global channel, which can be used directly during testing to
 * write date to.
 */
class TappedWriter(args: Arguments) : Processor(args) {
  /** Writer which is exposed to the pipeline. */
  private val output: SendChannel<ByteArray> = arguments["output"]

  /** Continuously read data from the global channel and write it to the output. */
  override suspend fun exec() {
    output.send(input.receive())
    input.close()
  }

  companion object {
    /** Global channel from which all data is read. */
    val input = Channel<ByteArray>()

    /** Implementation of this processor as IR. */
    val processor =
        IRProcessor(
            "tapped_writer",
            "https://www.rdf-connect.com/#JVMRunner",
            null,
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
          "tapped_writer_stage", processor.uri, mapOf("output" to IRArgument(listOf(channelURI))))
    }
  }
}
