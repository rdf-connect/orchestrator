import { Processor } from "jvm-runner-ts";
import * as fs from "node:fs";

export default class FileWriter extends Processor {
  private incoming = this.args.get("incoming", {
    type: "reader",
    list: "false",
    nullable: "false",
  });

  private path = this.args.get("path", {
    type: "string",
    list: "false",
    nullable: "false",
  });

  async exec(): Promise<void> {
    while (true) {
      const data = await this.incoming.read();
      console.log(`Incoming data: ${data.toString()}`);
      fs.writeFileSync(this.path, data, { flag: "a" });
      console.log(`Written to: ${this.path}`);
    }
  }
}
