# Python Runner

## Development Guide

### gRPC Code Generation

```shell
python -m grpc_tools.protoc -I../../protos --python_out=. --pyi_out=. --grpc_python_out=. ../../protos/helloworld.proto
```
