import { Writer } from "./writer";
import { RunnerError } from "../error";

/**
 * A callback channel is a simple implementation of a writer that calls a
 * callback whenever a value is written to it. The class does therefore
 * not implement the `Reader` interface, as it is not possible to read from
 * the channel.
 */
export class CallbackChannel<T> implements Writer<T> {
  /**
   * The callback that is called whenever a value is written to the channel.
   * @private
   */
  private readonly callback: (value: T) => void | Promise<void>;

  /**
   * Whether the channel has been closed or not.
   * @private
   */
  private closed = false;

  /**
   * Create a new callback channel with a specific callback.
   * @param callback The callback to call whenever a value is written to the
   * channel.
   */
  constructor(callback: (value: T) => void | Promise<void>) {
    this.callback = callback;
  }

  close(): void {
    this.closed = true;
  }

  isClosed(): boolean {
    return false;
  }

  write(data: T): void {
    if (!this.closed) {
      this.callback(data);
    } else {
      RunnerError.channelError();
    }
  }
}
