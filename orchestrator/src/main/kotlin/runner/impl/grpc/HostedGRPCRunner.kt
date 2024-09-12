package technology.idlab.runner.impl.grpc

import java.io.File
import kotlin.random.Random
import kotlin.random.nextUInt
import runner.impl.grpc.Config
import technology.idlab.extensions.rawPath
import technology.idlab.intermediate.IRRunner
import technology.idlab.intermediate.IRStage
import technology.idlab.util.ManagedProcess

/**
 * A hosted GRPC runner will create a new child process which contains the gRPC server, and manage
 * it accordingly.
 */
class HostedGRPCRunner
private constructor(
    /** The process which contains the gRPC server. */
    process: ManagedProcess,
    /** The configuration of the gRPC server. */
    config: Config,
    /** Which stages to load. */
    stages: Collection<IRStage>,
) : GRPCRunner(config, stages) {
  // Exit the gRPC runner client when the server exits.
  init {
    process.exitHook { this@HostedGRPCRunner.exit() }
  }

  companion object {
    /**
     * A GRPCRunner that runs a GRPC server in a child process.
     *
     * @param runner The runner to create a new instance of.
     * @param stages The stages to load in the runner.
     */
    fun create(runner: IRRunner, stages: Collection<IRStage>): HostedGRPCRunner {
      // Create a new config for the runner. We run the server on a random port in [5000-10000] for
      // now, but should be configurable later.
      val port = (Random.nextUInt(5000u, 10_000u))
      val config = Config(runner.uri, "127.0.0.1", port.toInt())

      // Configure and build the process.
      val cmd = "${runner.entrypoint} localhost $port"
      val builder = ProcessBuilder(cmd.split(" "))
      val dir = File(runner.directory!!.rawPath())
      builder.directory(dir)

      // Start the process.
      val process = ManagedProcess.from(builder, prettyLog = false)

      // Create a new runner.
      return HostedGRPCRunner(process, config, stages)
    }
  }
}
