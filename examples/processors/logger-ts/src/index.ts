import { ProcessorDefinition } from "jvm-runner-ts";
import { Processor } from "jvm-runner-ts";
import { Reader } from "jvm-runner-ts";
import { Writer } from "jvm-runner-ts";

@ProcessorDefinition("std/logger")
export class Logger extends Processor {
  private input = this.getArgument<Reader>("input");
  private output = this.getArgument<Writer>("output");

  override async exec(): Promise<void> {
    // eslint-disable-next-line no-constant-condition
    while (true) {
      // Read value from incoming stream.
      const value = await this.input.read();

      // Check if the value is present.
      if (value === null) {
        break;
      }

      // Write to console.
      console.log(value);

      // Write to outgoing stream.
      this.output.write(value);
    }
  }
}
