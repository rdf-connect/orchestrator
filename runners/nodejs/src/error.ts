export class RunnerError extends Error {
  private constructor(message: string) {
    super(message);
    this.name = "RunnerError";
  }

  static inconsistency(message: string | null = null): never {
    let msg = "An error occurred while parsing incoming data.";
    if (message) {
      msg += "\n" + message;
    }
    throw new RunnerError(msg);
  }

  static missingArgument(key: string): never {
    throw new RunnerError(`Missing argument: ${key}`);
  }

  static incorrectType(key: string, type: string): never {
    throw new RunnerError(`Incorrect type '${type}' for argument: ${key}`);
  }

  static nonExhaustiveSwitch(): never {
    throw new RunnerError("Non-exhaustive switch statement");
  }

  static missingImplementation(): never {
    throw new RunnerError("Not implemented");
  }

  static channelError(): RunnerError {
    return new RunnerError("Channel error");
  }

  static unexpectedBehaviour(): never {
    throw new RunnerError("Unexpected behaviour");
  }
}
