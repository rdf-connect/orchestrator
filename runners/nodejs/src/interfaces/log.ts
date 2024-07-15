import { Subject } from "rxjs";
import { LogEntry, LogLevel } from "../proto";

/**
 * Simple wrapper class which exposes an observable to which log messages are
 * written.
 */
export class Log {
  // Internal stream to which log messages are written.
  private stream = new Subject<LogEntry>();

  /**
   * Write a message to the log stream with the INFO level.
   * @param message The message to write, either as literal or a function
   * returning a string.
   */
  info(message: string | (() => string)): void {
    if (message instanceof Function) {
      message = message();
    }

    this.push({ level: LogLevel.INFO, message });
  }

  /**
   * Write a message to the log stream with the DEBUG level.
   * @param message The message to write, either as literal or a function
   */
  debug(message: string | (() => string)): void {
    if (message instanceof Function) {
      message = message();
    }

    this.push({ level: LogLevel.DEBUG, message });
  }

  /**
   * Write a message to the log stream with the SEVERE level.
   * @param message The message to write.
   */
  severe(message: string): void {
    this.push({ level: LogLevel.SEVERE, message });
  }

  /**
   * Write a fatal message, indicating that the program must halt afterward.
   * @param message The message to write.
   */
  fatal(message: string): void {
    this.push({ level: LogLevel.FATAL, message });
  }

  /**
   * Add a new subscriber to the log stream.
   * @param next The callback to call when a new log message is pushed.
   */
  subscribe(next: (value: LogEntry) => void): void {
    this.stream.subscribe(next);
  }

  /**
   * Push a new log message to the stream. Automatically retrieves the function
   * name and line number.
   * @param value The log message to push.
   * @private
   */
  private push(value: { level: LogLevel; message: string }): void {
    this.stream.next({
      ...value,
      location: this.getCaller(),
    });
  }

  /**
   * Retrieve the caller of the log message by parsing the stack trace.
   * Returns as a simple string in the format `class::function::line`.
   * @private
   */
  private getCaller(): string {
    // Create an error and extract the stack trace to retrieve the line number.
    const error = new Error();
    const stackLines = error.stack?.split("\n");
    if (!stackLines || stackLines.length < 5) {
      return "NA";
    }

    // Typically, the caller is the third line in the stack trace
    const callerStackLine = stackLines[4];

    // Extract the function name and line number
    const match = callerStackLine.match(/at (\S+) \((.+):(\d+):\d+\)/);
    if (!match) {
      return "NA";
    }

    // Retrieve the function name and line number
    const func = match[1].replace(".", "::");
    const line = match[3];
    return `${func}::${line}`;
  }

  // A shared log instance makes it easy to log messages from anywhere in the code.
  static shared = new Log();
}
