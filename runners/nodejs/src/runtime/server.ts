import { ChannelData, RunnerServer } from "../proto";
import {
  sendUnaryData,
  ServerDuplexStream,
  ServerUnaryCall,
  UntypedHandleCall,
} from "@grpc/grpc-js";
import { IRProcessor, IRStage } from "../proto/intermediate";
import { Empty } from "../proto/empty";
import { Runner } from "./runner";

export class ServerImplementation implements RunnerServer {
  [name: string]: UntypedHandleCall;

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

  prepareStage(
    call: ServerUnaryCall<IRStage, Empty>,
    callback: sendUnaryData<Empty>,
  ): void {
    Runner.shared
      .prepareStage(call.request)
      .then(() => {
        callback(null, {});
      })
      .catch((e) => {
        callback(e, {});
      });
  }

  prepareProcessor(
    call: ServerUnaryCall<IRProcessor, Empty>,
    callback: sendUnaryData<Empty>,
  ): void {
    Runner.shared
      .prepareProcessor(call.request)
      .then(() => {
        callback(null, {});
      })
      .catch((e) => {
        callback(e, {});
      });
  }

  exec(
    call: ServerUnaryCall<Empty, Empty>,
    callback: sendUnaryData<Empty>,
  ): void {
    Runner.shared.exec().then(() => {
      callback(null, {});
    });
  }
}
