# RDF-Connect Orchestrator

Welcome to the main repository of the RDF-Connect Orchestrator. This project attempts to facilitate RDF-Connect pipelines between any combination of programming languages and runtimes. Extensibility is at the core of this project, allowing you to bring RDF-Connect to new languages using existing or new methods of interprocess communication.

## Getting Started

### Vocabulary

RDF-Connect introduces a fair amount of new concepts, each with their own terms and names. Below is a brief overview of the most important things to know before heading in.

- **Orchestrator**: the main control pane of an RDF-Connect pipeline, responsible for starting runners and processors as well as message brokering between different pipeline stages.
- **Runner**: a process which interprets the received control commands from the orchestrator and executes those to load processors, instantiate them into pipeline stages, and handles incoming and outgoing messages. Typically, a runner specializes in a specific programming language or runtime. For example, our standard Python runner can import and instantiate any kind of Python processor as long as it conforms to the expected abstract class.
- **Processor**: an implementation of some logic. These can be arbitrarily simple or complex, ranging from reading a file from disk to inference of a machine learning model. Processors can accept type-safe arguments, including destination-agnostic readers and writers.
- **Readers and Writers**: simple wrapper objects (or similar) which provide simple APIs to interact with the messaging system, such as asynchronous `send` and `receive` methods.
- **Stage**: a processor which has been instantiated with concrete arguments and reader/writer targets.

Some lingo specific to the packaging and distributing is the following.

- **Package**: a collection of runners and processors, published locally or online.
- **Dependency**: a package required to load and execute a pipeline.
- **Target**: the runner which should execute a given processor.
- **Prepare statements**: commands which must be executed to build the runners and processors in a package and create the runtime environment if required. For example, executing a compilation step using the build system.

### Installation

#### Docker

We provide a convenient and lean Docker image to get started quickly. Kotlin processors are supported out-of-the-box, but you may need to extend this image to host certain targets, such as `node` and `npm` for the TypeScript runner.

```shell
docker pull ghcr.io/rdf-connect/orchestrator:latest
```

#### macOS Homebrew

This repository contains a [homebrew formula](./rdfc.rb).

```shell
brew install rdf-connect/orchestrator https://github.com/rdf-connect/orchestrator
```

### Standard Package Library

We provide a number of runners and processors to fast-track the development of pipelines, as well as to provide a reference on how to implement your own. All of these are exported by our [`index.ttl`](./index.ttl) file, and can be loaded using a single dependency. Learn more [here](./packages).

```turtle
<MyPipeline> rdfc:dependency <https://github.com/rdf-connect/orchestrator.git> .
```

## Contributors

### Submodules

The Protobuf declarations are available in a separate repository. Make sure to initialize its respective submodule, since otherwise your Gradle build will fail.

```shell
git submodule update --init --recursive
```

You can of course also use the `--recurse-submodules` switch with `git clone`.

### Github Authentication

Some of the pipelines found in this repository depend on repositories hosted on GitHub, requiring an authentication token. These can be configured using the environment variables listed in `.env.example`. It suffices to copy this file to `.env` and fill in the variables.

### Publishing

This project automatically publishes Maven packages and a Docker image whenever the `projectVersion` field in [`gradle.properties`](./gradle.properties) is updated.

### Git Hooks

This repository uses [`pre-commit`](https://pre-commit.com) for automatic linting and formatting. To get started, run the following command.

```shell
pre-commit install
```

Conventional commits are enforced, but require an additional command to register the hook.

```shell
pre-commit install --hook-type commit-msg
```

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
