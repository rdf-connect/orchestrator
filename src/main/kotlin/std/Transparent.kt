package technology.idlab.std

import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.SendChannel
import technology.idlab.runner.impl.jvm.Arguments
import technology.idlab.runner.impl.jvm.Processor
import technology.idlab.util.Log

class Transparent(args: Arguments) : Processor(args) {
  private val input: ReceiveChannel<ByteArray> = arguments["input"]
  private val output: SendChannel<ByteArray> = arguments["output"]

  override suspend fun exec() {
    Log.shared.debug { "Transparent processor started" }
    for (data in input) {
      Log.shared.debug { "Received ${data.size} bytes" }
      output.send(data)
    }
    output.close()
    Log.shared.debug { "Transparent processor finished" }
  }
}
