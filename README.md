# JVM Runner

[![Test Suite](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml/badge.svg)](https://github.com/rdf-connect/jvm-runner/actions/workflows/test.yml)

A proof-of-concept implementation of a Kotlin-based JVM runner.

#### Notes

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