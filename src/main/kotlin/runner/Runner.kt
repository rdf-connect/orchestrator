package technology.idlab.runtime

import kotlinx.coroutines.channels.Channel
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage
import technology.idlab.runtime.impl.NodeRunner
import technology.idlab.runtime.jvm.JVMRunner
import technology.idlab.util.Log

abstract class Runner {
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

  /** Every runtime comes with its own bidirectional channel for reading and writing messages. */
  protected val incoming = Channel<Pair<String, ByteArray>>()
  protected val outgoing = Channel<Pair<String, ByteArray>>()

  /** Register and prepare a processor inside the runtime. */
  abstract suspend fun prepare(processor: IRProcessor)

  /** Register and prepare a stage inside the runtime. */
  abstract suspend fun prepare(stage: IRStage)

  /** Start pipeline execution. */
  abstract suspend fun exec()

  /** Return the current state of the runtime. */
  abstract suspend fun status(): Status

  companion object {
    /* Runtimes are provided as singletons, but are lazily initialized. */
    private val runtimes = mutableMapOf<Target, Runner>()

    /* Create a new runtime. Note this will not save it in the targets. */
    private fun create(target: Target): Runner {
      return when (target) {
        Target.JVM -> JVMRunner()
        Target.NODEJS -> NodeRunner()
      }
    }

    /** Get the runtime for a specific target. */
    fun get(target: Target): Runner {
      return runtimes.getOrPut(target) { create(target) }
    }
  }
}
