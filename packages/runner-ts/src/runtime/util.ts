import { RunnerError } from "../error";

/**
 * Convert an object to a Map of strings to a generic type.
 * @param object The object to convert.
 */
export function asMap<T>(object: { [key: string]: T }): Map<string, T> {
  return new Map(Object.entries(object));
}

/**
 * Attempt to execute a function, and return its result. If the function fails
 * the program should panic.
 * @param func The function to execute.
 */
export function tryOrPanic<T>(func: () => T): T | never {
  try {
    return func();
  } catch (e) {
    RunnerError.unexpectedBehaviour();
  }
}
