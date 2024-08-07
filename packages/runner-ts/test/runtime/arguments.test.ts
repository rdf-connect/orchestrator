import { describe, expect, test } from "vitest";
import { Arguments } from "../../src/runtime/arguments";

describe("Arguments", () => {
  test("nullable", () => {
    expect.assertions(1);

    const input = new Map<string, unknown[]>();
    const args = new Arguments(input);

    const result = args.get("name", {
      type: "boolean",
      list: "false",
      nullable: "true",
    });

    expect(result).toBeNull();
  });

  test("boolean - single as single", () => {
    expect.assertions(1);

    const input = new Map<string, unknown[]>();
    input.set("key", [true]);
    const args = new Arguments(input);

    const result = args.get("key", {
      type: "boolean",
      list: "false",
      nullable: "false",
    });

    expect(result).toBeTruthy();
  });

  test("boolean - single as list", () => {
    expect.assertions(2);

    const input = new Map<string, unknown[]>();
    input.set("key", [true]);
    const args = new Arguments(input);

    const result = args.get("key", {
      type: "boolean",
      list: "true",
      nullable: "false",
    });

    expect(result).toHaveLength(1);
    expect(result.at(0) ?? false).toBeTruthy();
  });

  test("boolean - many as list", () => {
    expect.assertions(3);

    const input = new Map<string, unknown[]>();
    input.set("key", [true, true]);
    const args = new Arguments(input);

    const result = args.get("key", {
      type: "boolean",
      list: "true",
      nullable: "false",
    });

    expect(result).toHaveLength(2);
    expect(result.at(0) ?? false).toBeTruthy();
    expect(result.at(1) ?? false).toBeTruthy();
  });

  test("boolean - many as single", () => {
    expect.assertions(1);

    const input = new Map<string, unknown[]>();
    input.set("key", [true, true]);
    const args = new Arguments(input);

    expect(() => {
      args.get("key", {
        type: "boolean",
        list: "false",
        nullable: "false",
      });
    }).toThrowError();
  });

  test("boolean - error from string", () => {
    expect.assertions(1);

    const input = new Map<string, unknown[]>();
    input.set("key", ["Hello, World!"]);
    const args = new Arguments(input);

    expect(() => {
      args.get("key", {
        type: "boolean",
        list: "false",
        nullable: "false",
      });
    }).toThrowError();
  });

  test("nested", () => {
    expect.assertions(1);

    const input = new Map<string, unknown[]>();
    const innerInput = new Map<string, unknown[]>();
    innerInput.set("inner", [true]);
    input.set("outer", [innerInput]);

    const args = new Arguments(input);
    const innerArgs = args.get("outer", {
      type: "map",
      list: "false",
      nullable: "false",
    });

    const result = innerArgs.get("inner", {
      type: "boolean",
      list: "false",
      nullable: "false",
    });

    expect(result).toBeTruthy();
  });
});
