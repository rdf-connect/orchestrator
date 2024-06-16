export class JVMRunnerError extends Error {
  constructor(message: string) {
    super(message);
    this.name = "JVMRunnerError";
  }

  static missingArgument(key: string): JVMRunnerError {
    return new JVMRunnerError(`Missing argument: ${key}`);
  }

  static missingImplementation(): JVMRunnerError {
    return new JVMRunnerError("Not implemented");
  }

  static channelError(): JVMRunnerError {
    return new JVMRunnerError("Channel error");
  }

  static unexpectedBehaviour(): JVMRunnerError {
    return new JVMRunnerError("Unexpected behaviour");
  }
}
