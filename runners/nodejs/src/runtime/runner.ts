import { IRParameterType, IRStage } from "../proto/intermediate";
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

  async load(stage: IRStage): Promise<void> {
    /** Load the processor into Node.js */
    const absolutePath = path.resolve(stage.processor!.metadata.import);
    const processor = await import(absolutePath);
    const constructor = processor.default;

    /** Parse the stage's arguments. */
    const args: Map<String, unknown> = new Map();
    Object.entries(stage.arguments).map(([key, arg]) => {
      if (arg.parameter!.type == IRParameterType.READER) {
        const subject = new Subject<Uint8Array>();
        const reader = new Reader(subject);
        this.readers.set(arg.value[0], subject);
        args.set(arg.parameter!.name, reader);
      } else if (arg.parameter!.type == IRParameterType.WRITER) {
        const subject = new Subject<Uint8Array>();
        subject.subscribe((data) => {
          this.outgoing.next({
            destinationUri: arg.value[0],
            data: data,
          });
        });
        const writer = new Writer(subject);
        args.set(arg.parameter!.name, writer);
      } else {
        console.error(
          new Error(`Unsupported parameter type ${arg.parameter!.type}`),
        );
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

  async halt(): Promise<void> {
    console.log("Halting stages.");
    this.incomingSubscription.unsubscribe();
  }

  static shared = new Runner();
}
