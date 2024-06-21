import { Processor } from "../interfaces/processor";
import { Reader } from "../interfaces/reader";
import { Writer } from "../interfaces/writer";

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
      this.output.write(data);
    }
  }
}
