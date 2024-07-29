import { RunnerServer } from "../proto";
import {
  sendUnaryData,
  ServerDuplexStream,
  ServerUnaryCall,
  UntypedHandleCall,
} from "@grpc/grpc-js";
import { IRStage } from "../proto/intermediate";
import { Empty } from "../proto/empty";
import { Runner } from "./runner";
import { ChannelMessage } from "../proto/channel";

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
  exec(call: ServerDuplexStream<ChannelMessage, ChannelMessage>): void {
    // Start execution, and end the call on success.
    Runner.shared.exec().then(() => {
      call.end();
    });

    // On incoming data, call the appropriate reader.
    call.on("data", function (payload: ChannelMessage) {
      Runner.shared.incoming.write(payload);
    });

    // On outgoing data, write it to the stream.
    (async () => {
      for await (const data of Runner.shared.outgoing) {
        call.write(data);
      }
    })();
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
        console.error(e);
        callback(null, {});
      });
  }
}
