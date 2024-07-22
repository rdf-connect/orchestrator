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
  private readonly onWrite: (value: T) => Promise<void>;

  /**
   * A callback to execute when the channel is closed. Note that this will wait
   * for the remaining data to be written before closing the channel.
   * @private
   */
  private readonly onClose: null | (() => Promise<void>) = null;

  /**
   * Whether the channel has been closed or not.
   * @private
   */
  private closed = false;

  /**
   * Create a new callback channel with a specific callback.
   * @param onWrite The callback to call whenever a value is written to the
   * @param onClose An optional callback to execute when the channel is closed.
   * channel.
   */
  constructor(
    onWrite: (value: T) => Promise<void>,
    onClose: (() => Promise<void>) | null = null,
  ) {
    this.onWrite = onWrite;
    this.onClose = onClose;
  }

  close(): Promise<void> {
    // Channels cannot be closed twice.
    if (this.closed) {
      RunnerError.channelError();
    }

    this.closed = true;

    // Execute the callback.
    if (this.onClose) {
      return this.onClose();
    } else {
      return Promise.resolve();
    }
  }

  isClosed(): boolean {
    return this.closed;
  }

  write(data: T): Promise<void> {
    if (!this.closed) {
      return this.onWrite(data);
    } else {
      throw RunnerError.channelError();
    }
  }
}
