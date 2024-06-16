export class RunnerError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "JVMRunnerError";
  }

  static missingArgument(key: string): RunnerError {
    return new RunnerError(`Missing argument: ${key}`);
  }

  static missingImplementation(): RunnerError {
    return new RunnerError("Not implemented");
  }

  static channelError(): RunnerError {
    return new RunnerError("Channel error");
  }

  static unexpectedBehaviour(): RunnerError {
    return new RunnerError("Unexpected behaviour");
  }
}
