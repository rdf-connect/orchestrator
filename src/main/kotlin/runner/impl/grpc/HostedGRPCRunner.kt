package technology.idlab.runner.impl.grpc

import java.io.File
import kotlin.random.Random
import kotlin.random.nextUInt
import kotlinx.coroutines.channels.Channel
import technology.idlab.extensions.rawPath
import technology.idlab.util.ManagedProcess

/**
 * A hosted GRPC runner will create a new child process which contains the gRPC server, and manage
 * it accordingly.
 */
class HostedGRPCRunner
private constructor(
    /** The channel to send messages to. */
    fromProcessors: Channel<Payload>,
    /** The process which contains the gRPC server. */
    process: ManagedProcess,
    /** The configuration of the gRPC server. */
    config: Config,
) : GRPCRunner(fromProcessors, config) {
  // Exit the gRPC runner client when the server exits.
  init {
    process.exitHook { this@HostedGRPCRunner.exit() }
  }

  companion object {
    /**
     * A GRPCRunner that runs a GRPC server in a child process.
     *
     * @param command The command to invoke the GRPCRunner.
     * @param directory The working directory where the command will be executed.
     * @param fromProcessors The channel where the payloads are sent to from the processors.
     */
    fun create(
        command: String,
        directory: File,
        fromProcessors: Channel<Payload>
    ): HostedGRPCRunner {
      // Create a new config for the runner. We run the server on a random port in [5000-10000] for
      // now, but should be configurable later.
      val port = (Random.nextUInt(5000u, 10_000u))
      val config = Config("127.0.0.1", port.toInt())

      // Configure and build the process.
      val cmd = "$command localhost $port"
      val builder = ProcessBuilder(cmd.split(" "))
      val dir = File(directory.rawPath())
      builder.directory(dir)

      // Start the process.
      val process = ManagedProcess.from(builder)

      // Create a new runner.
      return HostedGRPCRunner(fromProcessors, process, config)
    }
  }
}
