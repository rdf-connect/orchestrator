# -*- coding: utf-8 -*-
# Generated by the protocol buffer compiler.  DO NOT EDIT!
# source: channel.proto
# Protobuf Python Version: 5.26.1
"""Generated protocol buffer code."""
from google.protobuf import descriptor as _descriptor
from google.protobuf import descriptor_pool as _descriptor_pool
from google.protobuf import symbol_database as _symbol_database
from google.protobuf.internal import builder as _builder
# @@protoc_insertion_point(imports)

_sym_db = _symbol_database.Default()




DESCRIPTOR = _descriptor_pool.Default().AddSerializedFile(b'\n\rchannel.proto\x12\x04rdfc\"\x16\n\x07\x43hannel\x12\x0b\n\x03uri\x18\x01 \x01(\t\"\x1c\n\x0b\x43hannelData\x12\r\n\x05\x62ytes\x18\x01 \x01(\x0c\"\x87\x01\n\x0e\x43hannelMessage\x12\x1e\n\x07\x63hannel\x18\x01 \x01(\x0b\x32\r.rdfc.Channel\x12&\n\x04type\x18\x02 \x01(\x0e\x32\x18.rdfc.ChannelMessageType\x12$\n\x04\x64\x61ta\x18\x03 \x01(\x0b\x32\x11.rdfc.ChannelDataH\x00\x88\x01\x01\x42\x07\n\x05_data*)\n\x12\x43hannelMessageType\x12\x08\n\x04\x44\x41TA\x10\x00\x12\t\n\x05\x43LOSE\x10\x01\x62\x06proto3')

_globals = globals()
_builder.BuildMessageAndEnumDescriptors(DESCRIPTOR, _globals)
_builder.BuildTopDescriptorsAndMessages(DESCRIPTOR, 'channel_pb2', _globals)
if not _descriptor._USE_C_DESCRIPTORS:
  DESCRIPTOR._loaded_options = None
  _globals['_CHANNELMESSAGETYPE']._serialized_start=215
  _globals['_CHANNELMESSAGETYPE']._serialized_end=256
  _globals['_CHANNEL']._serialized_start=23
  _globals['_CHANNEL']._serialized_end=45
  _globals['_CHANNELDATA']._serialized_start=47
  _globals['_CHANNELDATA']._serialized_end=75
  _globals['_CHANNELMESSAGE']._serialized_start=78
  _globals['_CHANNELMESSAGE']._serialized_end=213
# @@protoc_insertion_point(module_scope)
