import { Processor } from "../interfaces/processor";

/**
 * The Transparent processor reads data and transmits it directly to it's output, while also logging the data to the
 * console. This processor is only used for debugging purposes.
 */
export default class Transparent extends Processor {
  // The channel to read from.
  private readonly input = this.args.get("input", {
    type: "reader",
    list: "false",
    nullable: "false",
  });

  // The channel to write to.
  private readonly output = this.args.get("output", {
    type: "writer",
    list: "false",
    nullable: "false",
  });

  async exec(): Promise<void> {
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const data = await this.input.read();
      if (!data) {
        break;
      }
      this.output.write(data);
    }
  }
}
