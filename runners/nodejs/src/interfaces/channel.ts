import { RunnerError } from "../error";
import { Reader } from "./reader";
import { Writer } from "./writer";

/**
 * A channel is a communication mechanism that allows for the transfer of values
 * between two endpoints. This is a simple implementation of a channel that
 * allows for the writing and reading of values.
 */
export class Channel<T> implements Reader<T>, Writer<T> {
  /**
   * The values stored in the channel, which are buffered here if there is no
   * reader to consume them.
   * @private
   */
  private readonly values: Array<T> = [];

  /**
   * Outstanding reads that are waiting for a value to be written to the channel.
   * @private
   */
  private readonly reads: Array<(value: T) => void> = [];

  /**
   * Whether the channel has been closed or not.
   * @private
   */
  private closed: boolean = false;

  /**
   * Write a value to the channel. If there is a reader waiting for a value, it
   * will be resolved immediately. Otherwise, the value will be buffered.
   * @param value The value to write to the channel.
   */
  write(value: T) {
    if (this.closed) {
      RunnerError.channelError();
    }

    const read = this.reads.shift();

    if (read) {
      read(value);
    } else {
      this.values.push(value);
    }
  }

  /**
   * Read a value from the channel. If there is a value buffered, it will be
   * resolved immediately. Otherwise, the read will be buffered.
   */
  read(): Promise<T> {
    return new Promise((resolve, reject) => {
      const result = this.values.shift();

      if (result) {
        resolve(result);
      } else if (this.closed) {
        reject(RunnerError.channelError());
      } else {
        this.reads.push(resolve);
      }
    });
  }

  /**
   * Close the channel, preventing any further writes from occurring.
   */
  close() {
    this.closed = true;
  }

  /**
   * Check if the channel is closed or not.
   */
  isClosed(): boolean {
    return this.closed;
  }
}
