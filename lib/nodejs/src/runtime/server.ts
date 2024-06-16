import {
  ArgumentType,
  Payload,
  ProcessorDefinitions,
  RunnerServer,
  Stage,
  Stages,
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
import { JVMRunnerError } from "../error";
import { Reader } from "../interfaces/reader";

/* Keep track of all writers and readers below. */
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
      throw JVMRunnerError.unexpectedBehaviour();
    });
  }

  getProcessorDefinitions(
    call: ServerUnaryCall<Void, ProcessorDefinitions>,
    callback: sendUnaryData<ProcessorDefinitions>,
  ): void {
    callback(null, {
      paths: [...Processor.getProcessors().keys()],
    });
  }

  setup(
    call: ServerUnaryCall<Stages, Void>,
    callback: sendUnaryData<Void>,
  ): void {
    call.on("data", function (stages: Stages): void {
      initStages(stages);
      callback(null, {});
    });
  }
}

export function initStages(stages: Stages): Processor[] {
  return stages.stages.map((stage) => initStage(stage));
}

export function initStage(stage: Stage): Processor {
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
          JVMRunnerError.channelError();
        },
        complete: () => {
          JVMRunnerError.unexpectedBehaviour();
        },
      };

      const writer = new Writer(observer);
      parsedArgs.set(name, writer);
      continue;
    }

    // If the argument is not a reader or writer, it is a value.
    parsedArgs.set(name, argument.value);
  }

  const processor = Processor.getProcessors().get(stage.processorUri);
  if (!processor) {
    throw JVMRunnerError.missingImplementation();
  }
  return new processor(parsedArgs);
}
