# RDF-Connect Orchestrator

Welcome to the main repository of the RDF-Connect Orchestrator. This project attempts to facilitate RDF-Connect pipelines between any combination of programming languages and runtimes. Extensibility is at the core of this project, allowing you to bring RDF-Connect to new languages using existing or new methods of interprocess communication.

## Installation

### Docker

We provide a convenient and lean Docker image to get started quickly. Kotlin processors are supported out-of-the-box, but you may need to extend this image to host certain targets, such as `node` and `npm` for the TypeScript runner.

```shell
docker pull ghcr.io/rdf-connect/orchestrator:latest
```

### macOS Homebrew

This repository contains a [homebrew formula](./rdfc.rb).

```shell
brew install rdf-connect/orchestrator https://github.com/rdf-connect/orchestrator
```

## Contributors

### Code Style and Formatting

Due to the high amount of languages in this repository, we provide [a simple shell script](./format.sh) which formats most (if not all) files in this project. Please make sure your contributions conform to these formatters at all times.

### Project Structure

The following section aims to give you an initial understanding of the project structure as well as provide motivation for certain design decisions. Note however that this project makes use of [KDoc](https://kotlinlang.org/docs/kotlin-doc.html), so API and implementation details are available separately. This document only covers the project conceptually.

#### Command line interface - [`rdfc-cli`](rdfc-cli)

This is the only module which contains an executable, and it's scope is extremely limited. All different execution modes (such as `install`, `validate`, `exec`) are mapped to a respective function, which in turn calls into the other `rdfc` libraries. If at one point a function in this package provides a large amount of functionality, moving it into a different module should be considered.

The `rdfc-cli` module can also be seen as a [facade](https://en.wikipedia.org/wiki/Facade_pattern), since it wraps many aspects of the orchestrator into convenient and simple function calls.

#### Utility code - [`rdfc-core`](rdfc-core)

This module is a collection of wrapper classes, extensions, and utility code.

#### Intermediate Representation - [`rdfc-intermediate`](rdfc-intermediate)

A collection of data classes which together model an RDF-Connect configuration.

The classes in this module are prefixed with `IR`, which stands for [intermediate representation](https://en.wikipedia.org/wiki/Intermediate_representation). We're taking some liberties with the definition here, but essentially we refer to the fact that (aside from parsing) we never execute queries against the RDF model itself. Rather, we extract the values as soon as possible into these data classes to achieve better separation of concern.

#### Orchestrator - [`rdfc-orchestrator`](rdfc-orchestrator)

`rdfc-orchestrator` is the heart of the project. It takes responsibility of the following tasks.

1. Accept tbe intermediate representation of a pipeline as parameter.
2. Instantiate the runners listed in that pipeline.
3. Forward pipeline stage declarations to the respective runners.
4. Facilitate message brokering during the execution of the pipeline, including control messages such as `Close Channel`.

Communication typically passes four distinct components. A message is a tuple of raw bytes, and it's target URI.

1. **Processor #1**: a processor can use a writer to submit a message to the system. It will forward the message to the runner.
2. **Runner #1**: receives the message from a processor and forwards it to the orchestrator stub. Any protocol can be used here, but at the time of writing the project provides support for gRPC only.
3. **Orchestrator Stub #1**: receives the message from the runner and forwards it to the central broker.
4. **Central Broker**: receives the message , attempts to match the destination URI to a stub, and forwards it.
5. **Orchestrator Stub #2**: receives the message from the broker and forwards it to it's corresponding runner.
6. **Runner #2**: receives the message from the stub and attempts to match the URI against a specific processor to forward to.
7. **Processor #2**: receives the message from the runner and buffers it in the corresponding reader.

Note that in the Kotlin runner, the stub and runner are implemented side-by-side in a single class.

#### Parser - [`rdfc-parser`](rdfc-parser)

Responsible for parsing a configuration to intermediate representation. The interface does not specify what type of configuration language that must be used, and instead only exposes methods which take in a file path as parameter. By default, only RDF in the Turtle syntax is supported.

#### Processor - [`rdfc-processor`](rdfc-processor)

This module exposes an abstract class which Kotlin-based processors must extend for the default runner implementation.
