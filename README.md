> [!CAUTION]
> This software is undergoing major changes. Documentation may be outdated.

# JVM Runner

[![Test Suite](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml/badge.svg)](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml)

A proof-of-concept implementation of a Kotlin-based JVM runner. Currently, this runner supports Java and Kotlin based processors.

### Features

#### Standard Processor Library

This runner includes a set of standard processors that can be used directly by the end user without the need for additional dependencies. These processors also serve as a reference for processor developers. The implementation can be found [here](src/main/kotlin/std).

##### RDF Utilities

Interact with RDF data.

| Processor                                                   | Description                    |
|-------------------------------------------------------------|--------------------------------|
| [`jvm:RDFValidator`](./src/main/kotlin/std/RDFValidator.kt) | Validate RDF data using SHACL. |

##### Network Utilities

These processors interact with the network.

| Processor                                             | Description                       |
|-------------------------------------------------------|-----------------------------------|
| [`jvm:HttpFetch`](./src/main/kotlin/std/HttpFetch.kt) | Reads data from an HTTP endpoint. |

##### File Utilities

Fetch and write data from and to the local file system.

| Processor                       | Description                                                            |
|---------------------------------|------------------------------------------------------------------------|
| [`jvm:FileReader`](./src/main/kotlin/std/FileReader.kt) | Reads a file with a given `path` from the local file system.           |
| [`jvm:FileWriter`](./src/main/kotlin/std/FileWriter.kt) | Overwrites/appends a file with a given `path` using the incoming data. |

#### Datatypes

At the time of writing, the JVM Runner supports a limit set of datatypes and classes. You may use either wrapper classes or the primitive types directly.

| URI                       | Java Mapping       |
|---------------------------|--------------------|
| `jvm:HttpChannelReader`   | `bridge.Reader`    |
| `jvm:HttpChannelWriter`   | `bridge.Writer`    |
| `jvm:MemoryChannelReader` | `bridge.Reader`    |
| `jvm:MemoryChannelWriter` | `bridge.Writer`    |
| `xsd:boolean`             | `boolean`          |
| `xsd:byte`                | `byte`             |
| `xsd:dateTime`            | `java.util.Date`   |
| `xsd:double`              | `double`           |
| `xsd:float`               | `float`            |
| `xsd:int`                 | `int`              |
| `xsd:long`                | `long`             |
| `xsd:string`              | `java.lang.String` |

Note that SHACL will validate your processor, so out-of-range or invalid values will be caught.

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
ktfmt ./**/*.kt
```

##### Java

Due to `ktfmt`'s relation with `google-java-format`, we use the later for Java code formatting. Invoke using the following command.

```shell
google-java-format -r ./**/*.java
```
