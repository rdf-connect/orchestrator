package technology.idlab.runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.impl.grpc.HostedGRPCRunner
import technology.idlab.runner.impl.jvm.JVMRunner
import technology.idlab.util.Log

abstract class Runner(protected val broker: Broker<ByteArray>) : BrokerClient<ByteArray> {
  /** A job to control the `CoroutineScope`. */
  private val job = Job()

  /** Scope made available to the runner where concurrency should be launched in. */
  protected val scope = CoroutineScope(job)

  /** The tasks to run concurrently, in a FIFO order. */
  private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)

  /**
   * All incoming tasks will be handled in a first-in first-out based, in order to guarantee their
   * order.
   */
  init {
    scope.launch {
      for (task in tasks) {
        task()
      }
    }
  }

  /** Schedule a new task to execute. */
  fun scheduleTask(task: suspend () -> Unit) {
    runBlocking { tasks.send(task) }
  }

  /** Register and prepare a stage inside the runtime. */
  open suspend fun load(processor: IRProcessor, stage: IRStage) {
    Log.shared.debug { "Loading stage '${stage.uri}' in '$uri'." }

    val writers = stage.getWriters(processor)
    val readers = stage.getReaders(processor)

    for (writer in writers) {
      broker.registerSender(writer)
    }

    for (reader in readers) {
      broker.registerReceiver(reader, this)
    }
  }

  /** Start pipeline execution. */
  abstract suspend fun exec()

  /** Attempt to exit the pipeline gracefully. */
  abstract suspend fun exit()

  companion object {
    private fun builtIn(uri: String, broker: Broker<ByteArray>): Runner {
      return when (uri) {
        "https://www.rdf-connect.com/#JVMRunner" -> JVMRunner(broker)
        else -> Log.shared.fatal("Unknown built in runner: $uri")
      }
    }

    fun from(runner: IRRunner, broker: Broker<ByteArray>): Runner {
      Log.shared.info("Creating runner: ${runner.uri}")

      when (runner.type) {
        IRRunner.Type.GRPC -> return HostedGRPCRunner.create(runner, broker)
        IRRunner.Type.BUILT_IN -> return builtIn(runner.uri, broker)
        else -> {
          Log.shared.fatal("Unknown runner type: ${runner.type}")
        }
      }
    }
  }
}
