import { Processor } from "jvm-runner-ts";
import * as fs from "node:fs";

export default class FileWriter extends Processor {
  private outgoing = this.args.get("outgoing", {
    type: "writer",
    list: "false",
    nullable: "false",
  });

  private path = this.args.get("path", {
    type: "string",
    list: "false",
    nullable: "false",
  });

  async exec(): Promise<void> {
    console.log(`Reading file: ${this.path}`);
    const data = fs.readFileSync(this.path);
    this.outgoing.write(data);
  }
}
