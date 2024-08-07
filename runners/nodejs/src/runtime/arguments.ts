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
import { Argument, ArgumentLiteral } from "../proto/intermediate";
import { ChannelRepository } from "./runner";
import { Log } from "../interfaces/log";

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
  private readonly args: { [key: string]: Argument };

  // Channel repository for instantiating readers and writers.
  private readonly repository: ChannelRepository;

  constructor(
    args: { [key: string]: Argument },
    repository: ChannelRepository,
  ) {
    this.args = args;
    this.repository = repository;
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
    // Check if the given argument exists, and handle accordingly.
    if (!Object.hasOwn(this.args, name)) {
      if (options.nullable === "true") {
        return null as Returns<T, L, N>;
      } else {
        RunnerError.missingArgument(name);
      }
    }

    // Retrieve the argument.
    const values = this.args[name];

    if (values.map) {
      if (options.type !== "map") {
        Log.shared.fatal(`Argument '${name}' is of type 'map'.`);
      }

      if (options.list !== "false") {
        Log.shared.fatal(`Argument '${name} is of type 'list'.`);
      }

      return new Arguments(values.map.values, this.repository) as Returns<
        T,
        L,
        N
      >;
    }

    if (values.maps) {
      if (options.type !== "map") {
        Log.shared.fatal(`Argument '${name}' is of type 'map'.`);
      }

      if (options.list !== "true") {
        Log.shared.fatal(`Argument '${name} is not of type 'list'.`);
      }

      return values.maps.values.map((element) => {
        return new Arguments(element.values, this.repository);
      }) as Returns<T, L, N>;
    }

    if (values.literal) {
      if (options.type === "map") {
        Log.shared.fatal(`Argument '${name}' is not of type 'map'.`);
      }

      if (options.list !== "false") {
        Log.shared.fatal(`Argument '${name} is not of type 'list'.`);
      }

      return this.getAs(values.literal, options.type) as Returns<T, L, N>;
    }

    if (values.literals) {
      if (options.type === "map") {
        Log.shared.fatal(`Argument '${name}' is not of type 'map'.`);
      }

      if (options.list !== "true") {
        Log.shared.fatal(`Argument '${name} is not of type 'list'.`);
      }

      return values.literals.values.map((literal) => {
        return this.getAs(literal, options.type);
      }) as Returns<T, L, N>;
    }

    Log.shared.fatal("No value present in Union.");
    process.exit(1);
  }

  private getAs(literal: ArgumentLiteral, type: Type) {
    if (type === "string") {
      return literal.string!;
    } else if (type === "boolean") {
      return literal.bool!;
    } else if (type === "reader") {
      return this.repository.createReader(literal.reader!.uri);
    } else if (type === "writer") {
      return this.repository.createWriter(literal.writer!.uri);
    } else if (type === "double" || type === "float") {
      return literal.double || literal.float!;
    } else if (type === "int" || type === "long") {
      return (
        literal.uint32 || literal.uint64 || literal.int32 || literal.int64!
      );
    } else if (type === "date") {
      return literal.timestamp!;
    } else {
      Log.shared.fatal("Non-exhaustive switch.");
    }
  }
}
