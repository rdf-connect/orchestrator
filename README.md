# JVM Runner

[![Test Suite](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml/badge.svg)](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml)

A proof-of-concept implementation of a Kotlin-based JVM runner.

### Features

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

###### Pre-Commit Hooks

This repository supports `pre-commit` hooks. To install the hooks, run the following command.

```shell
pre-commit install
```

###### Formatting

The code in this repository is formatted using Meta's `ktfmt` tool, mainly due to the following feature.

> `ktfmt` ignores most existing formatting. It respects existing newlines in some places, but in general, its output is deterministic and is independent of the input code.

No feature flags are used. Invoke using the following command.

```shell
ktfmt ./**/*.kt
```