import {
  IRArgument,
  IRParameter,
  IRParameterCount,
  IRParameterPresence,
  IRParameterType,
  IRStage,
} from "../proto/intermediate";
import { ChannelData } from "../proto";
import { Subject, Subscription } from "rxjs";
import { Processor } from "../interfaces/processor";
import * as path from "node:path";
import { Reader } from "../interfaces/reader";
import { Writer } from "../interfaces/writer";

export class Runner {
  /** Channels. */
  public incoming = new Subject<ChannelData>();
  public outgoing = new Subject<ChannelData>();
  private incomingSubscription: Subscription;
  private readers: Map<String, Subject<Uint8Array>> = new Map();

  /** Runtime config. */
  private stages: Map<String, Processor> = new Map();

  constructor() {
    this.incomingSubscription = this.incoming.subscribe((payload) => {
      const reader = this.readers.get(payload.destinationUri);
      if (!reader) {
        throw new Error(
          `Reader not found for payload ${payload.destinationUri}`,
        );
      }
      reader.next(payload.data);
    });
  }

  parseArgumentSimple(arg: IRParameterType, value: string): unknown {
    if (arg == IRParameterType.STRING) {
      return value;
    } else if (arg == IRParameterType.INT) {
      return Number.parseInt(value);
    } else if (arg == IRParameterType.FLOAT) {
      return Number.parseFloat(value);
    } else if (arg == IRParameterType.BOOLEAN) {
      return value == "true";
    } else if (arg == IRParameterType.READER) {
      const subject = new Subject<Uint8Array>();
      this.readers.set(value, subject);
      return new Reader(subject);
    } else if (arg == IRParameterType.WRITER) {
      const subject = new Subject<Uint8Array>();
      subject.subscribe((data) => {
        this.outgoing.next({
          destinationUri: value,
          data: data,
        });
      });
      return new Writer(subject);
    } else {
      throw new Error("Invalid argument type");
    }
  }

  parseArgument(arg: IRArgument, param: IRParameter): unknown[] {
    if (arg.complex && param.complex) {
      const params: Map<string, IRParameter> = new Map(
        Object.entries(param.complex.parameters),
      );
      return arg.complex.value.map((map) => {
        const args: Map<string, IRArgument> = new Map(
          Object.entries(map.arguments),
        );
        return this.parseArguments(args, params);
      });
    }

    if (arg.simple && param.simple) {
      return arg.simple.value.map((value) =>
        this.parseArgumentSimple(param.simple!, value),
      );
    }

    throw new Error("Invalid argument type");
  }

  parseArguments(
    args: Map<string, IRArgument>,
    params: Map<string, IRParameter>,
  ): Map<String, unknown> {
    const result = new Map<String, unknown>();

    for (const [name, arg] of args) {
      const param = params.get(name)!;
      const parsed = this.parseArgument(arg, param);

      if (param.count == IRParameterCount.SINGLE) {
        if (parsed.length > 1) {
          throw new Error(`Too many arguments for ${name}`);
        }

        result.set(name, parsed[0]);
      } else {
        result.set(name, parsed);
      }
    }

    for (const [name, param] of params) {
      if (param.presence == IRParameterPresence.REQUIRED && !result.has(name)) {
        throw new Error(`Missing required argument ${name}`);
      }
    }

    return result;
  }

  async load(stage: IRStage): Promise<void> {
    /** Load the processor into Node.js */
    const absolutePath = path.resolve(stage.processor!.metadata.import);
    const processor = await import(absolutePath);
    const constructor = processor.default;

    /** Parse the stage's arguments. */
    const args = new Map(Object.entries(stage.arguments));
    const params = new Map(Object.entries(stage.processor!.parameters!));
    const parsedArguments = this.parseArguments(args, params);

    try {
      const processorInstance = new constructor(parsedArguments);
      this.stages.set(stage.uri, processorInstance);
    } catch (e) {
      console.error(e);
    }
  }

  async exec(): Promise<void> {
    console.log("Executing stages.");
    this.stages.forEach((stage) => {
      new Promise(() => {
        try {
          return stage.exec();
        } catch (e) {
          console.error(e);
          throw e;
        }
      });
    });
  }

  static shared = new Runner();
}
