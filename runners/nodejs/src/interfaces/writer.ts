import { Observer } from "rxjs";
import { Log } from "./log";

export class Writer {
  private channel: Observer<Uint8Array>;

  constructor(channel: Observer<Uint8Array>) {
    this.channel = channel;
  }

  write(bytes: Uint8Array): void {
    Log.shared.debug(() => {
      const serialized = bytes.toString().replace("\n", "\\n");
      return `'${serialized}' -> [unknown]`;
    });

    this.channel.next(bytes);
  }
}
