package runner

import kotlin.concurrent.thread
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.runBlocking
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.util.Log

abstract class Runner(
    // Messages which are destined to a processor inside the runner.
    protected val incoming: Channel<Payload>,
    // Message which must be transmitted to the outside world.
    protected val outgoing: Channel<Payload>,
) {
  /** The state of a runtime. */
  enum class Status {
    STARTING,
    INITIALIZING,
    READY,
    RUNNING,
    FINISHED,
    ERROR,
    CRASHED,
  }

  /*
   * Implementations of this abstract class are exhaustively listed here. If you were to write your
   * own runtime implementation, you should add an entry here, as well as extend the companion
   * object.
   */
  enum class Target {
    JVM,
    NODEJS,
    ;

    companion object {
      /* Convert a string to a target. */
      fun fromString(value: String): Target {
        return when (value) {
          "JVM" -> JVM
          "NodeJS" -> NODEJS
          else -> Log.shared.fatal("Unknown target: $value")
        }
      }
    }
  }

  /** The contents of a channel message. */
  data class Payload(
      // The URI of the reader which the message was sent to.
      val destinationURI: String,
      // The data of the message.
      val data: ByteArray,
  )

  /** Incoming messages are delegated to sub channels. These are mapped by their URI. */
  protected val readers = mutableMapOf<String, Channel<ByteArray>>()

  // Handle incoming messages.
  private val handler = thread {
    runBlocking {
      for (message in this@Runner.incoming) {
        // Get the reader.
        val reader =
            readers[message.destinationURI]
                ?: Log.shared.fatal("Unknown reader: ${message.destinationURI}")

        // Push data to the reader.
        reader.send(message.data)
      }
    }
  }

  /** Register and prepare a processor inside the runtime. */
  abstract suspend fun prepare(processor: IRProcessor)

  /** Register and prepare a stage inside the runtime. */
  abstract suspend fun prepare(stage: IRStage)

  /** Start pipeline execution. */
  abstract suspend fun exec()

  /** Return the current state of the runtime. */
  abstract suspend fun status(): Status

  /** Halt the execution of the runtime and release all resources. */
  fun halt() {
    // TODO: Propagate halting signal to processors.
    handler.interrupt()
  }
}
