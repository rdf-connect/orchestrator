import { Processor } from "../interfaces/processor";
import { Constructor } from "./constructor";

export async function resolve(): Promise<Constructor<Processor>> {
  // Define variables.
  const src =
    "/Users/jens/Developer/technology.idlab.jvm-runner/examples/processors/logger-ts/build/index.js";
  const packageName = "";
  const className = "Logger";

  // Read file from disk.
  const module = await import(src);
  const constructor = module[className];

  // Return result.
  return constructor as Constructor<Processor>;
}
