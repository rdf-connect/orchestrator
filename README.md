# RDF Connect Orchestrator

[![Test Suite](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml/badge.svg)](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml)

The RDF Connect Orchestrator implements and bridges processor runners across environments and runtimes.

| Runtime | Status            | Notes                                                      |
| ------- | ----------------- | ---------------------------------------------------------- |
| Kotlin  | Ready for testing | Reference implementation, directly executed on own thread. |
| Java    | Unstable          | Requires a thread per processor.                           |
| Node.js | Unstable          | Reference gRPC implementation.                             |
| Python  | Planned           | None.                                                      |
| Rust    | Planned           | None.                                                      |

## Overview

### Parser

> \[!NOTE\]
> RDF Connect pipelines are typically written in RDF. At the time of writing, only the Turtle file format is supported, but other formats will be supported soon.

The first stage of the orchestrator is responsible for parsing the plain text configuration file into an intuitive and easy-to-use format. We call this the _intermediate representation_, as defined in our [Protobuf schema](./proto/intermediate.proto). This phase is strictly separated from any and all actual setup of the orchestrator and individual runners, and can therefore be customized easily by enforcing the [`Parser` interface](src/main/kotlin/parser/Parser.kt).

> \[!WARNING\]
> An extended explanation of the Protobuf schema is required.

### Initialisation

As part of the [gRPC interface](./proto/index.proto), any and all runners are required to implement the `load` function of the gRPC server. This takes in a single `IRStage`, which contains both the processor definition, as well as the untouched `String` representation of the stage's arguments.

It is the runners responsibility to bring the processor into the runtime, deserialize the arguments based on the parameter configuration, and call the processor's constructor.

> \[!NOTE\]
> Processors should not do any heavy lifting inside of their constructor. All actual computations should be done inside the `exec` function as defined in the interface of the corresponding runtime.

### Communication

Communication between two processors must pass the orchestrator, at least for the time being. It acts as a central hub and message broker, which means that the runners themselves are not responsible for most of the message routing.

Specifically, this means that a runner must pass an incoming message from the gRPC bidirectional `channel` stream to the correct reader, as well as send any outgoing message from any writer back into the `channel` function.

### Standard Processor Library

Anyone may create and publish their own processors. However, to get started quickly, we provide some helpful processors as part of the Standard Processor Library. These are included by default, and may serve as a reference to implement your own processors.

#### RDF Utilities

Interact with RDF data.

| Processor                                                    | Description                    |
| ------------------------------------------------------------ | ------------------------------ |
| [`conn:RDFValidator`](./src/main/kotlin/std/RDFValidator.kt) | Validate RDF data using SHACL. |

#### Network Utilities

These processors interact with the network.

| Processor                                              | Description                       |
| ------------------------------------------------------ | --------------------------------- |
| [`conn:HttpFetch`](./src/main/kotlin/std/HttpFetch.kt) | Reads data from an HTTP endpoint. |

#### File Utilities

Fetch and write data from and to the local file system.

| Processor                                                | Description                                                            |
| -------------------------------------------------------- | ---------------------------------------------------------------------- |
| [`conn:FileReader`](./src/main/kotlin/std/FileReader.kt) | Reads a file with a given `path` from the local file system.           |
| [`conn:FileWriter`](./src/main/kotlin/std/FileWriter.kt) | Overwrites/appends a file with a given `path` using the incoming data. |

### Datatypes

At the time of writing, we support a limited set of literal types. You may also use complex data structures, which will be represented as a map.

| URI            | Kotlin             | Node.js   |
| -------------- | ------------------ | --------- |
| `xsd:boolean`  | `boolean`          | `Boolean` |
| `xsd:byte`     | `byte`             |           |
| `xsd:dateTime` | `java.util.Date`   | `Date`    |
| `xsd:double`   | `double`           |           |
| `xsd:float`    | `float`            |           |
| `xsd:int`      | `int`              | `Number`  |
| `xsd:long`     | `long`             | `Number`  |
| `xsd:string`   | `java.lang.String` | `String`  |

Note that SHACL will validate your configuration, so out-of-range or invalid values will be caught.

### Contributor Guide

#### Language Versions

The only Kotlin version supported is `v1.9.22` for the time being, due to dependencies on the embedded compiler.

#### Pre-Commit Hooks

This repository supports `pre-commit` hooks. To install the hooks, run the following command.

```shell
pre-commit install
```

#### Formatting

##### Kotlin

The Kotlin code in this repository is formatted using Meta's `ktfmt` tool, mainly due to the following feature.

> `ktfmt` ignores most existing formatting. It respects existing newlines in some places, but in general, its output is deterministic and is independent of the input code.

No feature flags are used. Invoke using the following command.

```shell
$ ktfmt ./**/*.kt
```

##### Java

Due to `ktfmt`'s relation with `google-java-format`, we use the later for Java code formatting. Invoke using the following command.

```shell
$ google-java-format -r ./**/*.java
```

##### TypeScript

The Node.js runner provides a `npm` script to format and lint all code.

```shell
$ npm run format --prefix ./runners/nodejs
```
