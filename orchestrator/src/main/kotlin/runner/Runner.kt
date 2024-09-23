package technology.idlab.runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.impl.grpc.HostedGRPCRunner
import technology.idlab.runner.impl.jvm.JVMRunner
import technology.idlab.util.Log

abstract class Runner(
    /** The stages which the runner must execute. */
    protected val stages: Collection<IRStage>
) : BrokerClient<ByteArray> {
  /** A job to control the `CoroutineScope`. */
  private val job = Job()

  /** Scope made available to the runner where concurrency should be launched in. */
  protected val scope = CoroutineScope(job)

  /** The tasks to run concurrently, in a FIFO order. */
  private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)

  /** Reference to the broker, set via dependency injection. */
  final override lateinit var broker: Broker<ByteArray>

  /** The URIs the runner wants to listen to. */
  final override val receiving: Set<String> = this.stages.map { it.getReaders() }.flatten().toSet()

  /** The URIs the runners wants to send to. */
  final override val sending: List<String> = this.stages.map { it.getWriters() }.flatten()

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

  /** Start pipeline execution. */
  abstract suspend fun exec()

  /** Attempt to exit the pipeline gracefully. */
  abstract suspend fun exit()

  companion object {
    fun from(runner: IRRunner, stages: Collection<IRStage>): Runner {
      Log.shared.info("Creating runner: ${runner.uri}")

      if (runner.uri == "https://www.rdf-connect.com/#JVMRunner") {
        return JVMRunner(stages)
      }

      if (runner.type == IRRunner.Type.GRPC) {
        return HostedGRPCRunner.create(runner, stages)
      }

      Log.shared.fatal("Unknown runner type: ${runner.type}")
    }
  }
}
