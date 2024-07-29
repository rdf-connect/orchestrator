/**
 * Warning: this file contains magic! We use advanced TypeScript features to
 * provide a type-safe way to handle arguments in the runtime. We heavily rely
 * on conditional types for this, as well as literal types, since those do not
 * get erased during compilation.
 *
 * Unfortunately, this means that adding new types to the runner requires a bit
 * of work, and the corresponding types should be added in multiple places here.
 *
 * Preferably, we should find a way to use reified types, such as in Kotlin, so
 * the `Options` object is not needed. However, there doesn't seem to be a good
 * solution for that.
 */

import { Writer } from "../interfaces/writer";
import { Reader } from "../interfaces/reader";
import { RunnerError } from "../error";
import { Channel } from "../interfaces/channel";
import { CallbackChannel } from "../interfaces/callback_channel";

/**
 * Argument types supported by RDF-Connect. These are enumerated as strings, in
 * order to support usage at runtime through literals.
 */
export type Type =
  | "boolean"
  | "byte"
  | "date"
  | "double"
  | "float"
  | "int"
  | "long"
  | "string"
  | "writer"
  | "reader"
  | "map";

/**
 * Map a type to its native Node.js type.
 */
type GetType<T extends Type> = T extends "boolean"
  ? boolean
  : T extends "byte"
    ? number
    : T extends "date"
      ? Date
      : T extends "double"
        ? number
        : T extends "float"
          ? number
          : T extends "int"
            ? number
            : T extends "long"
              ? number
              : T extends "string"
                ? string
                : T extends "writer"
                  ? Writer<Uint8Array>
                  : T extends "reader"
                    ? Reader<Uint8Array>
                    : T extends "map"
                      ? Arguments
                      : never;

/**
 * Check if a given value conforms to a given type.
 * @param value The value to check.
 * @param type The abstract type to check against.
 */
function conforms(value: unknown, type: Type): boolean {
  switch (type) {
    case "boolean":
      return typeof value === "boolean";
    case "byte":
      return typeof value === "number";
    case "date":
      return value instanceof Date;
    case "double":
      return typeof value === "number";
    case "float":
      return typeof value === "number";
    case "int":
      return typeof value === "number";
    case "long":
      return typeof value === "number";
    case "string":
      return typeof value === "string";
    case "writer":
      return value instanceof Channel || value instanceof CallbackChannel;
    case "reader":
      return value instanceof Channel;
    case "map":
      return value instanceof Arguments;
    default:
      RunnerError.nonExhaustiveSwitch();
  }
}

/**
 * Literal type which indicates if the requested type is a singleton or a list.
 */
export type List = "true" | "false";

/**
 * Literal type which indicate if the requested type is a nullable or not.
 */
export type Nullable = "true" | "false";

/**
 * Given a type `T`, return either `T` or `T[]` based on a `List` value.
 */
type GetList<T, L extends List | undefined> = L extends undefined
  ? T
  : L extends "true"
    ? T[]
    : T;

/**
 * Given a type `T`, return either `T` or `T?` depending on a `Nullable` value.
 */
type GetNullable<T, N extends Nullable | undefined> = N extends undefined
  ? T
  : N extends "true"
    ? T | null
    : T;

/**
 * Describes the return type of a returned argument function.
 */
export type Options<
  T extends Type,
  L extends List | undefined,
  N extends Nullable | undefined,
> = {
  type: T;
  list: L | undefined;
  nullable: N | undefined;
};

/**
 * Parse an `Options` type into a single concrete type.
 */
type Returns<
  T extends Type,
  L extends List | undefined,
  N extends Nullable | undefined,
> = GetNullable<GetList<GetType<T>, L>, N>;

/**
 * Wrapper class for processor arguments, which holds a string-to-any map and
 * provides a runtime-safe getter function.
 */
export class Arguments {
  // The actual arguments, parsed by the runner beforehand.
  private readonly args: Map<string, unknown[]>;

  constructor(args: Map<string, unknown[]>) {
    this.args = args;

    // Map all instances of a map into an `Arguments` object.
    for (const [key, values] of this.args) {
      const newValues = values.map((value) => {
        if (value instanceof Map) {
          return new Arguments(value);
        } else {
          return value;
        }
      });

      this.args.set(key, newValues);
    }
  }

  /**
   * Retrieve an argument in a type-safe manner using the provided options.
   * @param name The name of the argument.
   * @param options The options to use for parsing the argument regarding the
   * type, count, and presence.
   */
  get<
    T extends Type,
    L extends List | undefined,
    N extends Nullable | undefined,
  >(name: string, options: Options<T, L, N>): Returns<T, L, N> {
    const values = this.args.get(name);

    // If no value is found, handle accordingly.
    if (!values) {
      if (options.nullable === "true") {
        return null as Returns<T, L, N>;
      } else {
        RunnerError.missingArgument(name);
      }
    }

    // Cast the value to the correct type.
    values.forEach((value) => {
      if (!conforms(value, options.type)) {
        RunnerError.incorrectType(name, options.type);
      }
    });

    // If the value is a list, return it as such.
    if (options.list === "true") {
      return values as Returns<T, L, N>;
    }

    // Check if there is only one value present.
    if (values.length != 1) {
      RunnerError.inconsistency();
    }

    // Return the value.
    return values[0] as Returns<T, L, N>;
  }
}
