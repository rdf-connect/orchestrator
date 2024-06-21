import {
  IRParameter,
  IRParameterType,
  IRProcessor,
  IRStage,
} from "../proto/intermediate";
import { ChannelData } from "../proto";
import { Subject, Subscription } from "rxjs";
import { Processor } from "../interfaces/processor";
import { Constructor } from "./constructor";
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
  private processors: Map<
    String,
    { constructor: Constructor<Processor>; definition: IRProcessor }
  > = new Map();
  private stages: Map<String, Processor> = new Map();

  /** Executions as promises. */
  private readonly executions: Promise<void>[] = [];

  constructor() {
    this.incomingSubscription = this.incoming.subscribe((payload) => {
      console.log(`Incoming payload: ${payload.destinationUri}`);
      const reader = this.readers.get(payload.destinationUri);
      if (!reader) {
        throw new Error(
          `Reader not found for payload ${payload.destinationUri}`,
        );
      }
      reader.next(payload.data);
    });
  }

  async prepareProcessor(irProcessor: IRProcessor): Promise<void> {
    const absolutePath = path.resolve(irProcessor.metadata.import);
    console.log(`Importing ${absolutePath}`);
    const processor = await import(absolutePath);
    this.processors.set(irProcessor.uri, {
      constructor: processor.default,
      definition: irProcessor,
    });
  }

  async prepareStage(stage: IRStage): Promise<void> {
    console.log(
      `Preparing stage: \`${stage.uri}\` using ${stage.processorUri}`,
    );
    // Retrieve the processor definition and constructor.
    const entry = this.processors.get(stage.processorUri);
    if (entry === null || entry === undefined) {
      throw new Error(`Processor not found for stage ${stage.uri}`);
    }
    const { constructor, definition } = entry;

    // Retrieve parameters by name.
    const parameters: Map<String, IRParameter> = new Map();
    definition.parameters.forEach((param) => {
      parameters.set(param.name, param);
    });

    // Parse args.
    const args: Map<String, unknown> = new Map();
    stage.arguments.forEach((arg) => {
      const param = parameters.get(arg.name)!;
      if (param.type == IRParameterType.READER) {
        const subject = new Subject<Uint8Array>();
        const reader = new Reader(subject);
        this.readers.set(arg.value[0], subject);
        args.set(param.name, reader);
      } else if (param.type == IRParameterType.WRITER) {
        const subject = new Subject<Uint8Array>();
        subject.subscribe((data) => {
          this.outgoing.next({
            destinationUri: arg.value[0],
            data: data,
          });
        });
        const writer = new Writer(subject);
        args.set(param.name, writer);
      } else {
        console.error(new Error(`Unsupported parameter type ${param.type}`));
      }
    });

    try {
      const processorInstance = new constructor(args);
      this.stages.set(stage.uri, processorInstance);
    } catch (e) {
      console.error(e);
    }
  }

  async exec(): Promise<void> {
    console.log("Executing stages.");
    this.stages.forEach((stage) => {
      this.executions.push(
        new Promise(() => {
          try {
            return stage.exec();
          } catch (e) {
            console.error(e);
            throw e;
          }
        }),
      );
    });
  }

  async halt(): Promise<void> {
    console.log("Halting stages.");
    this.incomingSubscription.unsubscribe();
  }

  static shared = new Runner();
}
