import { RunnerError } from "../error";

const TIME_PADDING = 15;
const LEVEL_PADDING = 10;
const FILE_PADDING = 39;

enum LogLevel {
  DEBUG,
  INFO,
  SEVERE,
  FATAL,
}

/**
 * Simple wrapper class which exposes an observable to which log messages are
 * written.
 */
export class Log {
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
   * Write a fatal message to the log stream and do not return.
   */
  fatal(message: string): never {
    throw RunnerError.stageError(message);
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
   * Push a new log message to the stream. Automatically retrieves the function
   * name and line number.
   * @param value The log message to push.
   * @private
   */
  private push(value: { level: LogLevel; message: string }): void {
    const time = new Date()
      .toISOString()
      .slice(11, 22)
      .padEnd(TIME_PADDING, " ");
    const level = LogLevel[value.level].padEnd(LEVEL_PADDING, " ");
    const caller = this.getCaller().padEnd(FILE_PADDING, " ");
    const message = value.message;

    console.log(`${time}${level}${caller}${message}`);
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
    const file = match[2].split("/").pop();
    const line = match[3];
    return `${file}:${line}`;
  }

  // A shared log instance makes it easy to log messages from anywhere in the code.
  static shared = new Log();
}
