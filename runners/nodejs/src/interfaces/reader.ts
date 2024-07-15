import { firstValueFrom, Observable } from "rxjs";
import { RunnerError } from "../error";
import { Log } from "./log";

export class Reader {
  private channel: Observable<Uint8Array>;

  constructor(channel: Observable<Uint8Array>) {
    this.channel = channel;
  }

  async read(): Promise<Uint8Array> {
    try {
      const result = await firstValueFrom(this.channel.pipe());

      Log.shared.debug(() => {
        const serialized = result.toString().replace("\n", "\\n");
        return `[unknown] -> '${serialized}`;
      });

      return result;
    } catch (error) {
      throw RunnerError.channelError();
    }
  }
}
