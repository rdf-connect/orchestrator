# Standard Processor Library

This directory contains implementations of processors which can be used directly by the end user, without the need for additional dependencies. These also exist as a reference to processor developers.

Their RDF definitions can be found [here](./../../resources/std). These are included in the default ontology.

## Network Utilities

These processors interact with the network.

| Processor                     | Description                       |
|-------------------------------|-----------------------------------|
| [jvm:HttpFetch](HttpFetch.kt) | Reads data from an HTTP endpoint. |

## File Utilities

These processors interact with the local file system.

| Processor                       | Description                                                            |
|---------------------------------|------------------------------------------------------------------------|
| [jvm:FileReader](FileReader.kt) | Reads a file with a given `path` from the local file system.           |
| [jvm:FileWriter](FileWriter.kt) | Overwrites/appends a file with a given `path` using the incoming data. |
