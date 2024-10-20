# Orchestrator

This directory contains the source code of the RDF-Connect Orchestrator, an application to manage an RDF-Connect pipeline and which brokers messages between the individual runners.

## Modules

A key objective of the project is the separation of concern with respect to the individual modules. The parsing logic in [`parser`](rdfc-orchestrator/src/main/kotlin/parser), for example, is completely separated from all other modules. Of course, it relies on the data classes defined in the [`intermediate`](rdfc-orchestrator/src/main/kotlin/intermediate) module, but dependencies are minimized.

Most (and ideally all) modules contain interfaces at their root, such as the [`Runner`](rdfc-orchestrator/src/main/kotlin/runner/Runner.kt) class in the [`runner`](rdfc-orchestrator/src/main/kotlin/runner) package, with all implementations kept into their separate [`impl`](rdfc-orchestrator/src/main/kotlin/runner/impl) directory.

> \[!WARNING\]
> Inter-module dependencies may only import root-level interfaces such as mentioned above, and never concrete implementations. At the time of writing, this repository might not completely reflect this guideline.

### [`Broker`](rdfc-orchestrator/src/main/kotlin/broker)

The broker exposes two interfaces, namely the [`Broker`](rdfc-orchestrator/src/main/kotlin/broker/Broker.kt) and the [`BrokerClient`](rdfc-orchestrator/src/main/kotlin/broker/BrokerClient.kt). The former acts as a central messaging hub to which messages can be written and send to by the latter.

A soft requirement of the message broker is that the messages must be delivered on a FIFO basis.

### [`Exception`](rdfc-orchestrator/src/main/kotlin/exception)

Exposes the [`RunnerException`](rdfc-orchestrator/src/main/kotlin/exception/RunnerException.kt) class. At the time of writing, this class is underutilized and should be implemented more widely.

### [`Extensions`](rdfc-orchestrator/src/main/kotlin/extensions)

Provides Kotlin class extensions for a variety of classes, such as [`File`](rdfc-orchestrator/src/main/kotlin/extensions/File.kt). The name of the file indicates the class which is extended.

### [`Intermediate`](rdfc-orchestrator/src/main/kotlin/intermediate)

An "orchestrator-native" representation of the pipeline using Kotlin data classes. We parse the RDF-based pipeline definition as soon as possible into this intermediate representation to maximize separation of concern and developer convenience.

We deliberately choose to **not** use [IDL](https://en.wikipedia.org/wiki/Interface_description_language) generated code in order to minimize external dependencies and maximize flexibility. This does however mean that the intermediate representation must be parsed into yet another representation, such as when using gRPC. However, the overhead here is minimal, since this must only happen once during initialisation.

### [`Parser`](rdfc-orchestrator/src/main/kotlin/parser)

Implementing parsers comes with a great degree of freedom. The interface only requires an implementation to expose lists with the intermediate representation of the declared pipelines, processors, etc. Note, however, that parsing must happen lazily or during the constructor invocation.

In theory, there is no need to declare the configuration in an RDF-based manner. The orchestrator does expect URIs in its intermediate representation, however, but these can be arbitrary **unique** strings. This opens up possibilities for other file formats, such as JSON, but these are not officially supported.

### [`Resolver`](rdfc-orchestrator/src/main/kotlin/resolver)

Pipelines can declare dependencies, the source files of which may reside in an arbitrary location such as local directory or a remote Git repository. Of course, at one point we must retrieve those files and save them locally. The [`Resolver`](rdfc-orchestrator/src/main/kotlin/resolver/Resolver.kt) class does just that, exposing a function which takes in a dependency declaration and must return a `File` object pointing to the root of the retrieved package on disk.

It is **not** the resolvers job to prepare this package, however. It must merely retrieve it and save it locally.

### [`Runner`](rdfc-orchestrator/src/main/kotlin/runner)

TBA

### [`Utilities`](rdfc-orchestrator/src/main/kotlin/util)

This directory contains a variety of utility files and classes which are accessed throughout the project. They do not provide orchestrator-specific functionality.
