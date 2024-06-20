package technology.idlab.runner.impl

import RunnerGrpcKt
import io.grpc.ManagedChannelBuilder
import runner.Runner
import technology.idlab.parser.intermediate.IRProcessor
import technology.idlab.parser.intermediate.IRStage

/**
 * This runner has GRPC built-in, so the only configuration that an extending class needs to provide
 * is the host and port of the GRPC server, as well as actually booting the process.
 */
abstract class GRPCRunner(host: String, port: Int) : Runner() {
  /** Handle to the child process. */
  abstract val process: Process

  /** Create a single stub for all communication. */
  private val grpc: RunnerGrpcKt.RunnerCoroutineStub

  init {
    val builder = ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
    grpc = RunnerGrpcKt.RunnerCoroutineStub(builder)
  }

  override suspend fun prepare(processor: IRProcessor) {
    TODO("Not yet implemented.")
  }

  override suspend fun prepare(stage: IRStage) {
    TODO("Not yet implemented.")
  }

  override suspend fun exec() {
    TODO("Not yet implemented.")
  }

  override fun halt() {
    super.halt()
    TODO("Not yet implemented.")
  }

  override suspend fun status(): Status {
    TODO("Not yet implemented.")
  }
}
