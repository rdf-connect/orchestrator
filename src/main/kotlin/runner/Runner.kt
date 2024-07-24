package technology.idlab.runner

import technology.idlab.broker.Broker
import technology.idlab.broker.BrokerClient
import technology.idlab.intermediate.IRProcessor
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.runner.impl.grpc.HostedGRPCRunner
import technology.idlab.runner.impl.jvm.JVMRunner
import technology.idlab.util.Log

abstract class Runner(
    protected val broker: Broker<ByteArray>,
) : BrokerClient<ByteArray> {
  /** Register and prepare a stage inside the runtime. */
  abstract suspend fun load(processor: IRProcessor, stage: IRStage)

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
