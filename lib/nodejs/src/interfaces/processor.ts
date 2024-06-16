import { RunnerError } from "../error";

export class Processor {
  /* Retrieve a processor definition by its resource name. */
  private args: Map<string, unknown>;

  /* Parse the incoming arguments. */
  constructor(args: Map<string, unknown>) {
    this.args = args;
  }

  /* The actual implementation of the processor must be overridden here. */
  public exec(): void {
    throw RunnerError.missingImplementation();
  }

  /* Retrieve an argument. */
  public getArgument<T>(key: string): T {
    const result = this.args.get(key);

    if (!result) {
      throw RunnerError.missingArgument(key);
    }

    return result as T;
  }

  /* Retrieve an optional argument. */
  public getOptionalArgument<T>(key: string): T | null {
    return (this.args.get(key) ?? null) as T | null;
  }
}
