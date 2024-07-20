import { Writer } from "./writer";
import { RunnerError } from "../error";

/**
 * A buffered callback channel is a simple implementation of a writer that calls
 * a callback whenever a value is written to it. The class does therefore not
 * implement the `Reader` interface, as it is not possible to read from the
 * channel.
 *
 * The class buffers all values that are written to the channel before
 * the callback is set. Once the callback is set, all buffered values are
 * written to the callback.
 *
 * Note that the callback cannot be overwritten once it is set, and if the
 * channel is closed before a callback is set, an error is thrown.
 */
export class BufferedCallbackChannel<T> implements Writer<T> {
  /**
   * The buffer that stores the values written to the channel as long as there
   * is no callback set.
   * @private
   */
  private buffer: Array<T> = [];

  /**
   * The callback that is called whenever a value is written to the channel. If
   * it is not set, the values are buffered in the `buffer` array.
   * @private
   */
  private callback: null | ((value: T) => void | Promise<void>) = null;

  /**
   * Whether the channel has been closed or not.
   * @private
   */
  private closed = false;

  close(): void {
    // The channel was closed before a callback was set, which results in a loss of data.
    if (this.callback === null) {
      RunnerError.channelError();
    }

    this.closed = true;
  }

  isClosed(): boolean {
    return this.closed;
  }

  write(data: T): void {
    if (this.callback === null) {
      this.buffer.push(data);
    } else {
      this.callback(data);
    }
  }

  /**
   * Set the callback that is called whenever a value is written to the
   * channel. All values that were written to the channel before the callback
   * was set are written to the callback as well.
   * @param callback The callback to call whenever a value is written to the
   */
  setCallback(callback: (value: T) => void | Promise<void>): void {
    // The callback cannot be overwritten.
    if (this.callback != null) {
      RunnerError.channelError();
    }

    this.callback = callback;
    this.buffer.forEach((value) => callback(value));
    this.buffer = [];
  }
}
