// Code generated by protoc-gen-ts_proto. DO NOT EDIT.
// versions:
//   protoc-gen-ts_proto  v1.180.0
//   protoc               v5.27.0
// source: index.proto

/* eslint-disable */
import {
  type CallOptions,
  ChannelCredentials,
  Client,
  ClientDuplexStream,
  type ClientOptions,
  type ClientUnaryCall,
  handleBidiStreamingCall,
  type handleUnaryCall,
  makeGenericClientConstructor,
  Metadata,
  type ServiceError,
  type UntypedServiceImplementation,
} from "@grpc/grpc-js";
import { ChannelMessage } from "./channel.js";
import { Empty } from "./empty.js";
import { IRStage } from "./intermediate.js";

export const protobufPackage = "";

export type RunnerService = typeof RunnerService;
export const RunnerService = {
  load: {
    path: "/Runner/load",
    requestStream: false,
    responseStream: false,
    requestSerialize: (value: IRStage) => Buffer.from(IRStage.encode(value).finish()),
    requestDeserialize: (value: Buffer) => IRStage.decode(value),
    responseSerialize: (value: Empty) => Buffer.from(Empty.encode(value).finish()),
    responseDeserialize: (value: Buffer) => Empty.decode(value),
  },
  exec: {
    path: "/Runner/exec",
    requestStream: false,
    responseStream: false,
    requestSerialize: (value: Empty) => Buffer.from(Empty.encode(value).finish()),
    requestDeserialize: (value: Buffer) => Empty.decode(value),
    responseSerialize: (value: Empty) => Buffer.from(Empty.encode(value).finish()),
    responseDeserialize: (value: Buffer) => Empty.decode(value),
  },
  channel: {
    path: "/Runner/channel",
    requestStream: true,
    responseStream: true,
    requestSerialize: (value: ChannelMessage) => Buffer.from(ChannelMessage.encode(value).finish()),
    requestDeserialize: (value: Buffer) => ChannelMessage.decode(value),
    responseSerialize: (value: ChannelMessage) => Buffer.from(ChannelMessage.encode(value).finish()),
    responseDeserialize: (value: Buffer) => ChannelMessage.decode(value),
  },
} as const;

export interface RunnerServer extends UntypedServiceImplementation {
  load: handleUnaryCall<IRStage, Empty>;
  exec: handleUnaryCall<Empty, Empty>;
  channel: handleBidiStreamingCall<ChannelMessage, ChannelMessage>;
}

export interface RunnerClient extends Client {
  load(request: IRStage, callback: (error: ServiceError | null, response: Empty) => void): ClientUnaryCall;
  load(
    request: IRStage,
    metadata: Metadata,
    callback: (error: ServiceError | null, response: Empty) => void,
  ): ClientUnaryCall;
  load(
    request: IRStage,
    metadata: Metadata,
    options: Partial<CallOptions>,
    callback: (error: ServiceError | null, response: Empty) => void,
  ): ClientUnaryCall;
  exec(request: Empty, callback: (error: ServiceError | null, response: Empty) => void): ClientUnaryCall;
  exec(
    request: Empty,
    metadata: Metadata,
    callback: (error: ServiceError | null, response: Empty) => void,
  ): ClientUnaryCall;
  exec(
    request: Empty,
    metadata: Metadata,
    options: Partial<CallOptions>,
    callback: (error: ServiceError | null, response: Empty) => void,
  ): ClientUnaryCall;
  channel(): ClientDuplexStream<ChannelMessage, ChannelMessage>;
  channel(options: Partial<CallOptions>): ClientDuplexStream<ChannelMessage, ChannelMessage>;
  channel(metadata: Metadata, options?: Partial<CallOptions>): ClientDuplexStream<ChannelMessage, ChannelMessage>;
}

export const RunnerClient = makeGenericClientConstructor(RunnerService, "Runner") as unknown as {
  new (address: string, credentials: ChannelCredentials, options?: Partial<ClientOptions>): RunnerClient;
  service: typeof RunnerService;
  serviceName: string;
};
