# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: index.proto
# Protobuf Python Version: 5.26.1
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
from google.protobuf.internal import builder as _builder

# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()


import rdfc.proto.channel_pb2 as channel__pb2
import rdfc.proto.intermediate_pb2 as intermediate__pb2
from google.protobuf import empty_pb2 as google_dot_protobuf_dot_empty__pb2


DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(
    b"\n\x0bindex.proto\x12\x04rdfc\x1a\rchannel.proto\x1a\x12intermediate.proto\x1a\x1bgoogle/protobuf/empty.proto2m\n\x06Runner\x12+\n\x04load\x12\x0b.rdfc.Stage\x1a\x16.google.protobuf.Empty\x12\x36\n\x04\x65xec\x12\x14.rdfc.ChannelMessage\x1a\x14.rdfc.ChannelMessage(\x01\x30\x01\x62\x06proto3"
)

_globals = globals()
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, _globals)
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, "index_pb2", _globals)
if not _descriptor._USE_C_DESCRIPTORS:
    DESCRIPTOR._loaded_options = None
    _globals["_RUNNER"]._serialized_start = 85
    _globals["_RUNNER"]._serialized_end = 194
# @@protoc_insertion_point(module_scope)