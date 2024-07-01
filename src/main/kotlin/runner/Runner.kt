package runner

import kotlinx.coroutines.channels.Channel
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

abstract class Runner(
    val fromProcessors: Channel<Payload>,
) {
  /*
   * Implementations of this abstract class are exhaustively listed here. If you were to write your
   * own runtime implementation, you should add an entry here, as well as extend the companion
   * object.
   */
  enum class Target {
    JVM,
    NODEJS,
  }

  /** The contents of a channel message. */
  data class Payload(
      // The URI of the reader which the message was sent to.
      val channel: String,
      // The data of the message.
      val data: ByteArray,
  )

  /* Messages which are destined to a processor inside the runner. */
  val toProcessors = Channel<Payload>()

  /** Register and prepare a stage inside the runtime. */
  abstract suspend fun load(stage: IRStage)

  /** Start pipeline execution. */
  abstract suspend fun exec()

  /** Attempt to exit the pipeline gracefully. */
  open suspend fun exit() {
    Log.shared.debug("Closing channels.")
    fromProcessors.close()
    toProcessors.close()
  }
}
