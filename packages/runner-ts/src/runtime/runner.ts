import { Stage } from "../proto/intermediate";
import { Processor } from "../interfaces/processor";
import { Reader } from "../interfaces/reader";
import { Writer } from "../interfaces/writer";
import { Arguments } from "./arguments";
import { Log } from "../interfaces/log";
import { Channel } from "../interfaces/channel";
import { CallbackChannel } from "../interfaces/callback_channel";
import { ChannelMessage, ChannelMessageType } from "../proto/channel";
import assert from "node:assert";

export interface ChannelRepository {
  createReader(uri: String): Reader<Uint8Array>;
  createWriter(uri: String): Writer<Uint8Array>;
}

/**
 * The actual implementation of the runner, and the core of the program. It is
 * responsible for loading and executing stages, and managing the communication
 * between them. It is also responsible for parsing the arguments and binding
 * the reader and writer interfaces to the actual implementation.
 *
 * We provide a singleton as the static `Runner.shared` field, which is required
 * by the gRPC server since it is stateless.
 */
export class Runner implements ChannelRepository {
  // The incoming channel is bound to by an external object. Whenever data is
  // written to it, it is handled by the runner as an incoming message.
  public incoming = new CallbackChannel<ChannelMessage>(async (data) => {
    this.handleMessage(data);
  });

  // All writers are bound to the outgoing channel, and after it is written to,
  // the runner will delegate the messages to the server implementation.
  public outgoing = new Channel<ChannelMessage>();

  // Maps the URIs of channels to their corresponding readers. We use this map
  // to route incoming messages to their correct receiver.
  private readers: Map<String, Channel<Uint8Array>> = new Map();

  // We keep track of the stages that are loaded into the runner by URI. These
  // are instantiated beforehand and can be executed or interrupted.
  private stages: Map<String, Processor> = new Map();

  /**
   * Handle an incoming message by routing it to the correct reader. This is
   * done by looking up the destination URI in the readers map and calling the
   * next method on the corresponding subject.
   * @param payload The incoming message.
   */
  handleMessage(payload: ChannelMessage): void {
    const reader = this.readers.get(payload.channel!.uri!);
    if (!reader) {
      throw new Error(`Reader not found for payload ${payload.channel!.uri!}`);
    }

    // Check if the message is a close message, in which case we close the
    // reader and remove it from the map.
    if (payload.type == ChannelMessageType.CLOSE) {
      reader.close();
      return;
    }

    // The data message is the only other message type that is allowed.
    assert(payload.type == ChannelMessageType.DATA);
    reader.write(payload.data!.bytes!);
  }

  /**
   * Create a new writer for a specific channel URI. This writer is bound to the
   * outgoing channel, and whenever data is written to it, it is propagated to
   * the server implementation.
   * @param uri The channel to write to as a URI.
   * @private
   */
  createWriter(uri: string): Writer<Uint8Array> {
    return new CallbackChannel(
      async (data) => {
        this.outgoing.write({
          channel: {
            uri,
          },
          type: ChannelMessageType.DATA,
          data: {
            bytes: data,
          },
        });
      },
      async () => {
        this.outgoing.write({
          channel: {
            uri,
          },
          type: ChannelMessageType.CLOSE,
          data: undefined,
        });
      },
    );
  }

  /**
   * Create a new reader for a specific channel URI. This reader is bound to the
   * incoming channel, and whenever data is written to it, it is propagated to
   * the resulting reader.
   * @param uri The channel to read from as a URI.
   * @private
   */
  createReader(uri: string): Reader<Uint8Array> {
    const channel = new Channel<Uint8Array>();
    this.readers.set(uri, channel);
    return channel;
  }

  /**
   * Load a stage into the runner. It's processor will be instantiated with the
   * arguments provided in the stage, and the instance will be stored in the
   * stages map. Note that the execute function is not called here.
   * @param stage The stage to be instantiated.
   */
  async load(stage: Stage): Promise<void> {
    // Get the path of the processor.
    let path = stage.processor!.entrypoint;
    if (path.startsWith("file://")) {
      path = path.substring(7);
    }

    // Load the processor into Node.js.
    Log.shared.debug(() => `Importing processor: file://${path}`);
    const processor = await import(path);
    const constructor = processor.default;

    // Instantiate the processor.
    Log.shared.debug(() => `Instantiating stage: ${stage.uri}`);
    const args = new Arguments(stage.arguments, this);
    const instance = new constructor(args);

    // Keep track of it in the stages map.
    this.stages.set(stage.uri, instance);
  }

  /**
   * Execute all stages in the runner in parallel by calling the `exec` function
   * on all implementations. This function is asynchronous and will return once
   * all executions have been started.
   */
  async exec(): Promise<void> {
    // Execute all stages.
    const execs = [...this.stages.entries()].map(async ([uri, stage]) => {
      Log.shared.debug(() => `Executing stage: ${uri}`);
      await stage.exec();
      Log.shared.debug(() => `Finished stage: ${uri}`);
    });

    // Discard results.
    return Promise.all(execs).then(() => {});
  }

  /**
   * A shared runner instance, mainly used for stateless servers such as gRPC
   * which require the runner to be globally defined.
   */
  static shared = new Runner();
}
