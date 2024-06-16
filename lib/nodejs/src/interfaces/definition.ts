import { Processor } from "./processor";

export function ProcessorDefinition(resource: string) {
  return function (target: Function) {
    Processor.register(resource, target);
  };
}
