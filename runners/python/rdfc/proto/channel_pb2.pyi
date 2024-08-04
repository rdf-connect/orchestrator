from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Mapping as _Mapping, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class ChannelMessageType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    DATA: _ClassVar[ChannelMessageType]
    CLOSE: _ClassVar[ChannelMessageType]
DATA: ChannelMessageType
CLOSE: ChannelMessageType

class Channel(_message.Message):
    __slots__ = ("uri",)
    URI_FIELD_NUMBER: _ClassVar[int]
    uri: str
    def __init__(self, uri: _Optional[str] = ...) -> None: ...

class ChannelData(_message.Message):
    __slots__ = ("bytes",)
    BYTES_FIELD_NUMBER: _ClassVar[int]
    bytes: bytes
    def __init__(self, bytes: _Optional[bytes] = ...) -> None: ...

class ChannelMessage(_message.Message):
    __slots__ = ("channel", "type", "data")
    CHANNEL_FIELD_NUMBER: _ClassVar[int]
    TYPE_FIELD_NUMBER: _ClassVar[int]
    DATA_FIELD_NUMBER: _ClassVar[int]
    channel: Channel
    type: ChannelMessageType
    data: ChannelData
    def __init__(self, channel: _Optional[_Union[Channel, _Mapping]] = ..., type: _Optional[_Union[ChannelMessageType, str]] = ..., data: _Optional[_Union[ChannelData, _Mapping]] = ...) -> None: ...
