import { firstValueFrom, Observable } from "rxjs";
import { JVMRunnerError } from "../error";

export class Reader {
  private channel: Observable<Uint8Array>;

  constructor(channel: Observable<Uint8Array>) {
    this.channel = channel;
  }

  async read(): Promise<Uint8Array> {
    try {
      return firstValueFrom(this.channel.pipe());
    } catch (error) {
      throw JVMRunnerError.channelError();
    }
  }
}
