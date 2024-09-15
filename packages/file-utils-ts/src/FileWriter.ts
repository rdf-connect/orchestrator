import { Processor, Log } from "rdfc";
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
    // Remove the file prefix, since that is not valid in Node.js.
    if (this.path.startsWith("file://")) {
      this.path = this.path.slice(7);
    }

    // Remove file.
    try {
      fs.unlinkSync(this.path);
    } catch (e) {
      Log.shared.debug(() => `Could not remove file: ${this.path}`);
    }

    // Append all incoming data to the file.
    for await (const data of this.incoming) {
      Log.shared.debug(() => `Writing file: ${this.path}`);
      fs.writeFileSync(this.path, data, { flag: "a" });
    }
  }
}
