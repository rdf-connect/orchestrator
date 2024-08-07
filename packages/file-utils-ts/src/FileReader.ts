import { Processor, Log } from "jvm-runner-ts";
import * as fs from "node:fs";

export default class FileReader extends Processor {
  private outgoing = this.args.get("outgoing", {
    type: "writer",
    list: "false",
    nullable: "false",
  });

  private paths = this.args.get("paths", {
    type: "string",
    list: "true",
    nullable: "false",
  });

  async exec(): Promise<void> {
    for (const path of this.paths) {
      await this.readFile(path);
    }
    this.outgoing.close();
  }

  async readFile(path: string) {
    Log.shared.debug(() => `Reading file: ${path}`);

    // Remove the file prefix, since that is not valid in Node.js.
    if (path.startsWith("file://")) {
      path = path.slice(7);
    }

    let data: Buffer;
    try {
      data = fs.readFileSync(path);
    } catch (e) {
      if (e instanceof Error) {
        Log.shared.fatal(`Failed to read file: ${e.message}`);
      } else {
        Log.shared.fatal("Failed to read file");
      }
    }

    this.outgoing.write(data!);
  }
}
