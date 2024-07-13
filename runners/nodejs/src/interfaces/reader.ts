import { firstValueFrom, Observable } from "rxjs";
import { RunnerError } from "../error";

export class Reader {
  private channel: Observable<Uint8Array>;

  constructor(channel: Observable<Uint8Array>) {
    this.channel = channel;
  }

  async read(): Promise<Uint8Array> {
    try {
      const result = await firstValueFrom(this.channel.pipe());
      console.log(`[unknown] -> '${result.toString().replace("\n", "\\n")}'`);
      return result;
    } catch (error) {
      throw RunnerError.channelError();
    }
  }
}
