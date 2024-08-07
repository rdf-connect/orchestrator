import { RunnerError } from "../error";
import { Arguments } from "../runtime/arguments";

export abstract class Processor {
  /* Retrieve a processor definition by its resource name. */
  protected args: Arguments;

  /* Parse the incoming arguments. */
  constructor(args: Arguments) {
    this.args = args;
  }

  /* The actual implementation of the processor must be overridden here. */
  public async exec(): Promise<void> {
    throw RunnerError.missingImplementation();
  }
}
