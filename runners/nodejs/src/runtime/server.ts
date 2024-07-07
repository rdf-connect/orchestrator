import { ChannelData, RunnerServer } from "../proto";
import {
  sendUnaryData,
  ServerDuplexStream,
  ServerUnaryCall,
  UntypedHandleCall,
} from "@grpc/grpc-js";
import { IRStage } from "../proto/intermediate";
import { Empty } from "../proto/empty";
import { Runner } from "./runner";

/**
 * The implementation of the gRPC server. This class binds the incoming server
 * requests to the actual implementation of the Runner. It should not contain
 * any specific runner logic, in order to maintain flexibility in terms of which
 * communication protocol is used.
 */
export class ServerImplementation implements RunnerServer {
  [name: string]: UntypedHandleCall;

  /**
   * A duplex stream in which channel data can be written and read. These are
   * implemented by gRPC as callbacks, but can be easily bound to the runners
   * internal handlers.
   */
  channel(call: ServerDuplexStream<ChannelData, ChannelData>): void {
    // On incoming data, call the appropriate reader.
    call.on("data", function (payload: ChannelData) {
      console.log("gRPC::channel::data");
      Runner.shared.incoming.next(payload);
    });

    // On outgoing data, propagate to gRPC.
    Runner.shared.outgoing.subscribe((payload) => {
      console.log("gRPC::channel::write");
      call.write(payload);
    });
  }

  /**
   * Load a specific stage into the runner, without executing it. Once again, we
   * simply bind to the runners internal implementation.
   */
  load(
    call: ServerUnaryCall<IRStage, Empty>,
    callback: sendUnaryData<Empty>,
  ): void {
    Runner.shared
      .load(call.request)
      .then(() => {
        callback(null, {});
      })
      .catch((e) => {
        callback(e, {});
      });
  }

  /**
   * Execute all stages in the runner by calling the `exec` function on all
   * implementations.
   */
  exec(
    call: ServerUnaryCall<Empty, Empty>,
    callback: sendUnaryData<Empty>,
  ): void {
    console.log("gRPC::prepareProcessor::invoke");
    Runner.shared.exec().then(() => {
      console.log("gRPC::prepareProcessor::success");
      callback(null, {});
    });
  }
}
