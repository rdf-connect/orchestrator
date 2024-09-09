# Getting Started

This document provides a high level overview for developers which want to extend the RDF-Connect ecosystem by integrating new runtimes and environments, or create and release their own processors.

We want RDF-Connect to be as open and approachable as possible. Because of this, processors and runners have a large degree of freedom with respect to how they are implemented and published.

## Processor Development

The targeted runner largely decides how a processor should be implemented. Most commonly, these provide interfaces or abstract classes to which the processor must adhere to. For example, a TypeScript runner must extend the [`Processor`](./runners/nodejs/src/interfaces/processor.ts) class.

All runners should parse and present the intermediate representation in a ready-for-use format. For example, processor should easily be able to retrieve their arguments in a type-safe manner without having to parse them into their runtime's native representation. The Kotlin runner, for example, provides a simple [`Arguments`](./src/main/kotlin/runner/jvm/Arguments.kt) class which safely retrieves and casts the arguments to Kotlin objects.

The above includes readers and writers. Platform specific implementations, such as [Kotlin's `Channel`](https://kotlinlang.org/docs/channels.html) or libraries such as [RxJS](https://rxjs.dev) should be fully abstracted, leaving the processor with high level read and write interactions.

### Packaging

A packaged and distributed processor is nothing more than a simple directory containing an `index.ttl` file, which holds metadata and instructions on how to prepare and use the processor. In this way, it is much like the `package.json`, `build.gradle.kts`, and
even `Makefile`'s of the world.

An example for a TypeScript processor is given below. Note the `rdfc:prepare` step, which can be interpreted much like a `make` command invocation. It is responsible for taking the source files and converting them to a usable format for the runner.

```turtle
<MyProcessor>
  a rdfc:Processor;
  rdfc:version "1.3.4";
  rdfc:author "Jens Pots";
  rdfc:license "MIT";
  rdfc:prepare "npm run build";
  rdfc:entrypoint "dist/index.js":.

# Processor shape was emmitted.
```

### Distribution

At the time of writing, we set out to provide support for loading processors from the local file system, as well as from Git repositories. Usage in your pipelines should be as simple as the follows.

```turtle
# Local file dependency.
<pipeline> rdf:dependency <$HOME/processor-repo>.

# Git repository as a dependency.
<pipeline> rdf:dependency "https://github.com/rdf-connect/orchestrator.git".
```

We're investigating whether to store remote dependencies in the local directory, such as under `./rdfc-dependencies`, or under a shared cache directory like `$HOME/.rdfc/dependencies`.

## Runner Development

> \[!NOTE\]
> This is not a full guide, but rather a placeholder for you to get started while we are working on more thorough documentation.

Writing and publishing runners is more complex than processors, and have a greater degree of freedom in terms of implementation. Currently, the only supported communication protocol is gRPC, for which we've outlined a simple [server interface](./proto/index.proto).

In short, runners must facilitate the loading and preparing of processors into the runtime through the use of the `load` function, which takes in a stage's intermediate representation. Processors should be idled until the `exec` function is invoked. All messages should be handled by the `channel` function. Incoming messages are delegated to their corresponding processor, and outgoing messages are pushed into function, back to the client.
