import { Observer } from "rxjs";

export class Writer {
  private channel: Observer<Uint8Array>;

  constructor(channel: Observer<Uint8Array>) {
    this.channel = channel;
  }

  write(bytes: Uint8Array): void {
    this.channel.next(bytes);
  }
}
