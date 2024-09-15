import { Arguments, Processor, Reader } from "rdfc";

export default class Template extends Processor {
  private incoming = this.args.get("incoming", {
    type: "reader",
    list: "false",
    nullable: "false",
  });

  private outgoing = this.args.get("outgoing", {
    type: "writer",
    list: "false",
    nullable: "false",
  });

  constructor(args: Arguments) {
    super(args);
  }

  async exec() {
    for await (const chunk of this.incoming) {
      this.outgoing.write(chunk);
    }
    this.outgoing.close();
  }
}
