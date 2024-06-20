import { IRProcessor } from "../proto/intermediate";
import { ChannelData } from "../proto";
import { Subject } from "rxjs";

export class Runner {
  public incoming = new Subject<ChannelData>();
  public outgoing = new Subject<ChannelData>();

  prepareProcessor(processor: IRProcessor): void {
    throw new Error("Method not implemented");
  }

  prepareStage(stage: IRProcessor): void {
    throw new Error("Method not implemented");
  }

  exec(): void {
    throw new Error("Method not implemented");
  }

  static shared = new Runner();
}
