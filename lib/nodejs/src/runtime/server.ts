import {
  ArgumentType,
  Payload,
  Processor as AbstractProcessor,
  RunnerServer,
  Stage,
  Void,
} from "./runner";
import {
  sendUnaryData,
  ServerDuplexStream,
  ServerUnaryCall,
  UntypedHandleCall,
} from "@grpc/grpc-js";

import { Processor } from "../interfaces/processor";
import { Observable, Subject } from "rxjs";
import { Writer } from "../interfaces/writer";
import { RunnerError } from "../error";
import { Reader } from "../interfaces/reader";
import { Constructor } from "./constructor";
import { resolve } from "./resolve";

const processors: Map<string, Constructor<Processor>> = new Map();
const stages: Map<string, Processor> = new Map();
const readers: Map<string, Subject<Uint8Array>> = new Map();
const writers: Map<string, Observable<Uint8Array>> = new Map();

export class ServerImplementation implements RunnerServer {
  [name: string]: UntypedHandleCall;

  channel(call: ServerDuplexStream<Payload, Payload>): void {
    // On incoming data, call the appropriate reader.
    call.on("data", function (payload: Payload) {
      const reader = readers.get(payload.channelUri)!;
      reader.next(payload.data);
    });

    // On outgoing data, create a payload with the corresponding writer.
    for (const [uri, writer] of writers) {
      writer.subscribe((value) => {
        call.write({ data: value, channelUri: uri });
      });
    }

    // On end, throw an error.
    call.on("end", function () {
      throw RunnerError.unexpectedBehaviour();
    });
  }

  prepareStage(
    call: ServerUnaryCall<Stage, Void>,
    callback: sendUnaryData<Void>,
  ): void {
    call.on("data", function (stage: Stage): void {
      initStage(stage);
      callback(null, {});
    });
  }

  prepareProcessor(
    call: ServerUnaryCall<AbstractProcessor, Void>,
    callback: sendUnaryData<Void>,
  ): void {
    call.on("data", function (processor: AbstractProcessor): void {
      resolve().then((constructor) => {
        processors.set(processor.uri, constructor);
        callback(null, {});
      });
    });
  }

  exec(call: ServerUnaryCall<Void, Void>, callback: sendUnaryData<Void>): void {
    call.on("data", function (): void {
      processors.forEach((processor) => {
        new processor().exec();
      });
    });
  }
}

export function initStage(stage: Stage): void {
  const abstractArgs = Object.entries(stage.arguments);
  const parsedArgs = new Map<string, unknown>();

  for (const [name, argument] of abstractArgs) {
    if (argument.type == ArgumentType.READER) {
      const subject = new Subject<Uint8Array>();
      const reader = new Reader(subject);
      const uri = argument.value.toString();
      readers.set(uri, subject);
      parsedArgs.set(name, reader);
      continue;
    }

    if (argument.type == ArgumentType.WRITER) {
      const observer = {
        next: (value: Uint8Array) => {
          console.log("Next:", value);
        },
        error: () => {
          RunnerError.channelError();
        },
        complete: () => {
          RunnerError.unexpectedBehaviour();
        },
      };

      const writer = new Writer(observer);
      parsedArgs.set(name, writer);
      continue;
    }

    // If the argument is not a reader or writer, it is a value.
    parsedArgs.set(name, argument.value);
  }

  // Initialize the new stage.
  const constructor = processors.get(stage.processorUri)!;
  stages.set(stage.uri, new constructor(parsedArgs));
}
