import { Writer } from "./writer";
import { Channel } from "./channel";

/**
 * A callback channel is a simple implementation of a writer that calls a
 * callback whenever a value is written to it. The class does therefore
 * not implement the `Reader` interface, as it is not possible to read from
 * the channel.
 */
export class CallbackChannel<T> implements Writer<T> {
  /**
   * The flow of messages is implemented by wrapping a regular channel.
   * @private
   */
  private channel = new Channel<T>();

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
   * Handle incoming messages of the channel by piping them into the callback.
   * @private
   */
  private handler = new Promise(async () => {
    for await (const data of this.channel) {
      await this.onWrite(data)
    }

    if (this.onClose != null) {
      await this.onClose()
    }
  });

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
    this.channel.close()
    return Promise.resolve();
  }

  isClosed(): boolean {
    return this.channel.isClosed();
  }

  write(data: T) {
    this.channel.write(data)
  }
}
