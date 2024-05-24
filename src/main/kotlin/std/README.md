# Standard Runner Library

This directory contains implementations of processors which can be used directly by the end user, without the need for additional dependencies. These also exist as a reference to processor developers.

Their RDF definitions can be found [here](../../resources/std).

## File Utilities

These processors interact with the local file system.

| Processor                   | Description                                                            |
|-----------------------------|------------------------------------------------------------------------|
| [FileReader](FileReader.kt) | Reads a file with a given `path` from the local file system.           |
| [FileWriter](FileWriter.kt) | Overwrites/appends a file with a given `path` using the incoming data. |

