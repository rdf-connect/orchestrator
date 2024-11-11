package technology.idlab.rdfc.orchestrator.runner.grpc

import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder

/**
 * Define how the generic GRPCRunner should communicate over the network.
 *
 * @param uri The URI of the gRPC server.
 * @param host The host of the gRPC server.
 * @param port The port of the gRPC server.
 */
data class GRPCConfig(val uri: String, val host: String, val port: Int) {
  fun connect(): ManagedChannel {
    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build()
  }
}
