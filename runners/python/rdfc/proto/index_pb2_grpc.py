# Generated by the gRPC Python protocol compiler plugin. DO NOT EDIT!
"""Client and server classes corresponding to protobuf-defined services."""
import grpc
import warnings

import src.proto.channel_pb2 as channel__pb2
from google.protobuf import empty_pb2 as google_dot_protobuf_dot_empty__pb2
import src.proto.intermediate_pb2 as intermediate__pb2

GRPC_GENERATED_VERSION = '1.65.1'
GRPC_VERSION = grpc.__version__
EXPECTED_ERROR_RELEASE = '1.66.0'
SCHEDULED_RELEASE_DATE = 'August 6, 2024'
_version_not_supported = False

try:
    from grpc._utilities import first_version_is_lower
    _version_not_supported = first_version_is_lower(GRPC_VERSION, GRPC_GENERATED_VERSION)
except ImportError:
    _version_not_supported = True

if _version_not_supported:
    warnings.warn(
        f'The grpc package installed is at version {GRPC_VERSION},'
        + f' but the generated code in index_pb2_grpc.py depends on'
        + f' grpcio>={GRPC_GENERATED_VERSION}.'
        + f' Please upgrade your grpc module to grpcio>={GRPC_GENERATED_VERSION}'
        + f' or downgrade your generated code using grpcio-tools<={GRPC_VERSION}.'
        + f' This warning will become an error in {EXPECTED_ERROR_RELEASE},'
        + f' scheduled for release on {SCHEDULED_RELEASE_DATE}.',
        RuntimeWarning
    )


class RunnerStub(object):
    """Missing associated documentation comment in .proto file."""

    def __init__(self, channel):
        """Constructor.

        Args:
            channel: A grpc.Channel.
        """
        self.load = channel.unary_unary(
                '/rdfc.Runner/load',
                request_serializer=intermediate__pb2.Stage.SerializeToString,
                response_deserializer=google_dot_protobuf_dot_empty__pb2.Empty.FromString,
                _registered_method=True)
        self.exec = channel.stream_stream(
                '/rdfc.Runner/exec',
                request_serializer=channel__pb2.ChannelMessage.SerializeToString,
                response_deserializer=channel__pb2.ChannelMessage.FromString,
                _registered_method=True)


class RunnerServicer(object):
    """Missing associated documentation comment in .proto file."""

    def load(self, request, context):
        """Missing associated documentation comment in .proto file."""
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')

    def exec(self, request_iterator, context):
        """Missing associated documentation comment in .proto file."""
        context.set_code(grpc.StatusCode.UNIMPLEMENTED)
        context.set_details('Method not implemented!')
        raise NotImplementedError('Method not implemented!')


def add_RunnerServicer_to_server(servicer, server):
    rpc_method_handlers = {
            'load': grpc.unary_unary_rpc_method_handler(
                    servicer.load,
                    request_deserializer=intermediate__pb2.Stage.FromString,
                    response_serializer=google_dot_protobuf_dot_empty__pb2.Empty.SerializeToString,
            ),
            'exec': grpc.stream_stream_rpc_method_handler(
                    servicer.exec,
                    request_deserializer=channel__pb2.ChannelMessage.FromString,
                    response_serializer=channel__pb2.ChannelMessage.SerializeToString,
            ),
    }
    generic_handler = grpc.method_handlers_generic_handler(
            'rdfc.Runner', rpc_method_handlers)
    server.add_generic_rpc_handlers((generic_handler,))
    server.add_registered_method_handlers('rdfc.Runner', rpc_method_handlers)


 # This class is part of an EXPERIMENTAL API.
class Runner(object):
    """Missing associated documentation comment in .proto file."""

    @staticmethod
    def load(request,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.unary_unary(
            request,
            target,
            '/rdfc.Runner/load',
            intermediate__pb2.Stage.SerializeToString,
            google_dot_protobuf_dot_empty__pb2.Empty.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)

    @staticmethod
    def exec(request_iterator,
            target,
            options=(),
            channel_credentials=None,
            call_credentials=None,
            insecure=False,
            compression=None,
            wait_for_ready=None,
            timeout=None,
            metadata=None):
        return grpc.experimental.stream_stream(
            request_iterator,
            target,
            '/rdfc.Runner/exec',
            channel__pb2.ChannelMessage.SerializeToString,
            channel__pb2.ChannelMessage.FromString,
            options,
            channel_credentials,
            insecure,
            call_credentials,
            compression,
            wait_for_ready,
            timeout,
            metadata,
            _registered_method=True)
