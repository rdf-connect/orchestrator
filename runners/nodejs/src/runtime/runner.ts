import {
  IRArgument,
  IRParameter,
  IRParameterType,
  IRStage,
} from "../proto/intermediate";
import { Processor } from "../interfaces/processor";
import { Reader } from "../interfaces/reader";
import { Writer } from "../interfaces/writer";
import { RunnerError } from "../error";
import { asMap } from "./util";
import { Arguments } from "./arguments";
import { Log } from "../interfaces/log";
import { Channel } from "../interfaces/channel";
import { CallbackChannel } from "../interfaces/callback_channel";
import { ChannelMessage, ChannelMessageType } from "../proto/channel";
import assert from "node:assert";

/**
 * The actual implementation of the runner, and the core of the program. It is
 * responsible for loading and executing stages, and managing the communication
 * between them. It is also responsible for parsing the arguments and binding
 * the reader and writer interfaces to the actual implementation.
 *
 * We provide a singleton as the static `Runner.shared` field, which is required
 * by the gRPC server since it is stateless.
 */
export class Runner {
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
  private createWriter(uri: string): Writer<Uint8Array> {
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
   * @param channelURI The channel to read from as a URI.
   * @private
   */
  private createReader(channelURI: string): Reader<Uint8Array> {
    const channel = new Channel<Uint8Array>();
    this.readers.set(channelURI, channel);
    return channel;
  }

  /**
   * Parse a simple argument into its native Node.js representation. This is
   * done simply be exhaustively checking the type of the argument and parsing
   * the value accordingly.
   * @param type The type of the argument.
   * @param value The value of the argument.
   * @private
   */
  private parseSimpleArgument(type: IRParameterType, value: string): unknown {
    if (type == IRParameterType.BOOLEAN) {
      return value == "true";
    } else if (type == IRParameterType.BYTE) {
      return Number.parseInt(value);
    } else if (type == IRParameterType.DATE) {
      return new Date(value);
    } else if (type == IRParameterType.DOUBLE) {
      return Number.parseFloat(value);
    } else if (type == IRParameterType.FLOAT) {
      return Number.parseFloat(value);
    } else if (type == IRParameterType.INT) {
      return Number.parseInt(value);
    } else if (type == IRParameterType.LONG) {
      return Number.parseInt(value);
    } else if (type == IRParameterType.STRING) {
      return value;
    } else if (type == IRParameterType.WRITER) {
      return this.createWriter(value);
    } else if (type == IRParameterType.READER) {
      return this.createReader(value);
    } else {
      RunnerError.nonExhaustiveSwitch();
    }
  }

  /**
   * Parse a single parameter, either simple or complex, into its native Node.js
   * representation.
   * @param name The nam eof the argument.
   * @param arg The arguments to parse.
   * @param param The parameter to parse.
   */
  private parseArgument(
    name: string,
    arg: IRArgument,
    param: IRParameter,
  ): unknown[] {
    // If the argument is complex, we need to recursively parse the arguments.
    if (arg.complex != undefined && param.complex != undefined) {
      const params = asMap(param.complex.parameters);

      // Recursively call for each value.
      return arg.complex.value.map((map) => {
        const args = asMap(map.arguments);
        return this.parseComplexArgument(args, params);
      });
    }

    // If the argument is a single value, we can parse it directly.
    if (arg.simple != undefined && param.simple != undefined) {
      // Recursively call for each value.
      return arg.simple.value.map((value) =>
        this.parseSimpleArgument(param.simple!, value),
      );
    }

    // If the argument is not simple or complex, we throw an error.
    RunnerError.inconsistency("Expected simple or complex argument.");
  }

  /**
   * Parse incoming intermediate arguments into their native Node.js
   * representation. This is done with the help of the parameter map. Note that
   * we do not check for correctness, since the SHACL validator will already
   * have asserted that arguments are valid.
   * @param args The argument mapping.
   * @param params The parameter mapping.
   */
  private parseComplexArgument(
    args: Map<string, IRArgument>,
    params: Map<string, IRParameter>,
  ): Map<string, unknown[]> {
    // We gather the result into a new untyped map.
    const result = new Map<string, unknown[]>();

    // Simply go over all arguments and instantiate them, recursively
    // if required.
    for (const [name, arg] of args) {
      const param = params.get(name) ?? RunnerError.missingParameter(name);
      const parsed = this.parseArgument(name, arg, param);

      // Set the argument.
      result.set(name, parsed);
    }

    return result;
  }

  /**
   * Load a stage into the runner. It's processor will be instantiated with the
   * arguments provided in the stage, and the instance will be stored in the
   * stages map. Note that the execute function is not called here.
   * @param stage The stage to be instantiated.
   */
  async load(stage: IRStage): Promise<void> {
    // Get the path of the processor.
    let path = stage.processor!.entrypoint;
    if (path.startsWith("file://")) {
      path = path.substring(7);
    }

    // Load the processor into Node.js.
    Log.shared.debug(() => `Importing processor: file://${path}`);
    const processor = await import(path);
    const constructor = processor.default;

    // Parse the stage's arguments.
    const params =
      stage.processor?.parameters ?? RunnerError.missingParameters();
    const rawArguments = asMap(stage.arguments);
    const parsedArguments = this.parseComplexArgument(
      rawArguments,
      asMap(params),
    );

    // Instantiate the processor with the parsed arguments.
    Log.shared.debug(() => `Instantiating stage: ${stage.uri}`);
    const args = new Arguments(parsedArguments);
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
