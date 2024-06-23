import { Processor } from "../interfaces/processor";
import { Reader } from "../interfaces/reader";
import { Writer } from "../interfaces/writer";

/**
 * The Transparent processor reads data and transmits it directly to it's output, while also logging the data to the
 * console. This processor is only used for debugging purposes.
 */
export default class Transparent extends Processor {
  private readonly input = this.getArgument<Reader>("input");
  private readonly output = this.getArgument<Writer>("output");

  async exec(): Promise<void> {
    // eslint-disable-next-line no-constant-condition
    while (true) {
      const data = await this.input.read();
      if (!data) {
        break;
      }
      console.log(data.toString());
      this.output.write(data);
    }
  }
}
