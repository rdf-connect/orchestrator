import { ChannelData, LogEntry, RunnerServer } from "../proto";
import {
  sendUnaryData,
  ServerDuplexStream,
  ServerUnaryCall,
  ServerWritableStream,
  UntypedHandleCall,
} from "@grpc/grpc-js";
import { IRStage } from "../proto/intermediate";
import { Empty } from "../proto/empty";
import { Runner } from "./runner";
import { Log } from "../interfaces/log";

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
      Runner.shared.incoming.next(payload);
    });

    // On outgoing data, propagate to gRPC.
    Runner.shared.outgoing.subscribe((payload) => {
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
    Runner.shared.exec().then(() => {
      callback(null, {});
    });
  }

  /**
   * Handle all incoming log messages and send them to the client.
   */
  log(call: ServerWritableStream<Empty, LogEntry>): void {
    Log.shared.subscribe((entry) => {
      call.write(entry);
    });
  }
}
