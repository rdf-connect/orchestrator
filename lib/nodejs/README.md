# Node.js

In this directory, a runner and processor interface is defined for use with Node.js.

## Development Guide

### Preparations

We use `proto-ts` to generate TypeScript interfaces from the protocol buffer definitions. Run the following command to generate the source code.

```shell
protoc \
  --plugin=./node_modules/.bin/protoc-gen-ts_proto \
  --ts_proto_out=./src \
  --ts_proto_opt=outputServices=grpc-js \
  --proto-path=.. \
  ./runner.proto
```
