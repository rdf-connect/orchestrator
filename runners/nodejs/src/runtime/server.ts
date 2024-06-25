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

export class ServerImplementation implements RunnerServer {
  [name: string]: UntypedHandleCall;

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
