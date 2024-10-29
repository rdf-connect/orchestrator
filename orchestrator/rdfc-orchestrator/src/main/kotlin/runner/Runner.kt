package technology.idlab.rdfc.orchestrator.runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import technology.idlab.rdfc.core.intermediate.IRRunner
import technology.idlab.rdfc.core.intermediate.IRStage
import technology.idlab.rdfc.core.intermediate.runner.RunnerType
import technology.idlab.rdfc.orchestrator.broker.Broker
import technology.idlab.rdfc.orchestrator.broker.BrokerClient
import technology.idlab.rdfc.orchestrator.runner.exception.NoSuchRunnerException
import technology.idlab.rdfc.orchestrator.runner.exception.UnsupportedRunnerTypeException
import technology.idlab.rdfc.orchestrator.runner.grpc.GRPCRunner
import technology.idlab.rdfc.orchestrator.runner.jvm.JVMRunner

/**
 * A runner is responsible for executing a set of stages concurrently. A concrete implementation
 * must provide support for a specific type of architecture, such as Java Virtual Machine processors
 * or specific remote procedure call (RPC) protocols.
 *
 * All runners are registered as broker clients, meaning they can send and receive messages from the
 * orchestrator. The channels listened and send to are determined by the stages the runner is
 * responsible for. It is the runners responsibility to either propagate the messages to and from
 * the individual stages, either by executing them locally or by sending them to a remote location.
 *
 * @param stages The stages for which the runner is responsible.
 */
abstract class Runner(protected val stages: Collection<IRStage>) : BrokerClient<ByteArray> {
  /** A job to control the `CoroutineScope`. */
  private val job = Job()

  /** Scope made available to the runner where concurrency should be launched in. */
  protected val scope = CoroutineScope(job)

  /** The tasks to run concurrently, in a FIFO order. */
  private val tasks = Channel<suspend () -> Unit>(Channel.UNLIMITED)

  // BrokerClient implementation.
  final override lateinit var broker: Broker<ByteArray>
  final override val receiving = stages.map { it.readers() }.flatten()
  final override val sending = stages.map { it.writers() }.flatten()

  /*
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

  /**
   * Schedule a new task to execute in the local [scope].
   *
   * @param task The task to execute.
   */
  fun scheduleTask(task: suspend () -> Unit) {
    runBlocking { tasks.send(task) }
  }

  /** Start pipeline execution. */
  abstract suspend fun exec()

  /** Attempt to exit the pipeline gracefully. */
  abstract suspend fun exit()

  companion object {
    /**
     * Create a new runner based on the runner configuration. This function will create a new runner
     * and inject the stages which the runner must execute.
     *
     * @param runner The runner configuration.
     * @param stages The stages which the runner must execute.
     * @return A new runner instance.
     * @throws NoSuchRunnerException If the type of runner is not implemented.
     */
    fun from(runner: IRRunner, stages: Collection<IRStage>): Runner =
        when (runner.type) {
          RunnerType.BuiltIn -> {
            if (runner.uri == "https://www.rdf-connect.com/#JVMRunner") {
              JVMRunner(stages)
            } else {
              throw UnsupportedRunnerTypeException(runner.type)
            }
          }
          RunnerType.GRPC -> GRPCRunner.hostLocally(runner, stages)
        }
  }
}
