from google.protobuf import timestamp_pb2 as _timestamp_pb2
from google.protobuf.internal import containers as _containers
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Iterable as _Iterable, Mapping as _Mapping, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class Reader(_message.Message):
    __slots__ = ("uri",)
    URI_FIELD_NUMBER: _ClassVar[int]
    uri: str
    def __init__(self, uri: _Optional[str] = ...) -> None: ...

class Writer(_message.Message):
    __slots__ = ("uri",)
    URI_FIELD_NUMBER: _ClassVar[int]
    uri: str
    def __init__(self, uri: _Optional[str] = ...) -> None: ...

class ArgumentLiteral(_message.Message):
    __slots__ = ("bytes", "string", "bool", "double", "float", "int32", "int64", "uint32", "uint64", "timestamp", "reader", "writer")
    class List(_message.Message):
        __slots__ = ("values",)
        VALUES_FIELD_NUMBER: _ClassVar[int]
        values: _containers.RepeatedCompositeFieldContainer[ArgumentLiteral]
        def __init__(self, values: _Optional[_Iterable[_Union[ArgumentLiteral, _Mapping]]] = ...) -> None: ...
    BYTES_FIELD_NUMBER: _ClassVar[int]
    STRING_FIELD_NUMBER: _ClassVar[int]
    BOOL_FIELD_NUMBER: _ClassVar[int]
    DOUBLE_FIELD_NUMBER: _ClassVar[int]
    FLOAT_FIELD_NUMBER: _ClassVar[int]
    INT32_FIELD_NUMBER: _ClassVar[int]
    INT64_FIELD_NUMBER: _ClassVar[int]
    UINT32_FIELD_NUMBER: _ClassVar[int]
    UINT64_FIELD_NUMBER: _ClassVar[int]
    TIMESTAMP_FIELD_NUMBER: _ClassVar[int]
    READER_FIELD_NUMBER: _ClassVar[int]
    WRITER_FIELD_NUMBER: _ClassVar[int]
    bytes: bytes
    string: str
    bool: bool
    double: float
    float: float
    int32: int
    int64: int
    uint32: int
    uint64: int
    timestamp: _timestamp_pb2.Timestamp
    reader: Reader
    writer: Writer
    def __init__(self, bytes: _Optional[bytes] = ..., string: _Optional[str] = ..., bool: bool = ..., double: _Optional[float] = ..., float: _Optional[float] = ..., int32: _Optional[int] = ..., int64: _Optional[int] = ..., uint32: _Optional[int] = ..., uint64: _Optional[int] = ..., timestamp: _Optional[_Union[_timestamp_pb2.Timestamp, _Mapping]] = ..., reader: _Optional[_Union[Reader, _Mapping]] = ..., writer: _Optional[_Union[Writer, _Mapping]] = ...) -> None: ...

class ArgumentMap(_message.Message):
    __slots__ = ("values",)
    class ValuesEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: Argument
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[Argument, _Mapping]] = ...) -> None: ...
    class List(_message.Message):
        __slots__ = ("values",)
        VALUES_FIELD_NUMBER: _ClassVar[int]
        values: _containers.RepeatedCompositeFieldContainer[ArgumentMap]
        def __init__(self, values: _Optional[_Iterable[_Union[ArgumentMap, _Mapping]]] = ...) -> None: ...
    VALUES_FIELD_NUMBER: _ClassVar[int]
    values: _containers.MessageMap[str, Argument]
    def __init__(self, values: _Optional[_Mapping[str, Argument]] = ...) -> None: ...

class Argument(_message.Message):
    __slots__ = ("literal", "literals", "map", "maps")
    LITERAL_FIELD_NUMBER: _ClassVar[int]
    LITERALS_FIELD_NUMBER: _ClassVar[int]
    MAP_FIELD_NUMBER: _ClassVar[int]
    MAPS_FIELD_NUMBER: _ClassVar[int]
    literal: ArgumentLiteral
    literals: ArgumentLiteral.List
    map: ArgumentMap
    maps: ArgumentMap.List
    def __init__(self, literal: _Optional[_Union[ArgumentLiteral, _Mapping]] = ..., literals: _Optional[_Union[ArgumentLiteral.List, _Mapping]] = ..., map: _Optional[_Union[ArgumentMap, _Mapping]] = ..., maps: _Optional[_Union[ArgumentMap.List, _Mapping]] = ...) -> None: ...

class Processor(_message.Message):
    __slots__ = ("uri", "entrypoint", "metadata")
    class MetadataEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    URI_FIELD_NUMBER: _ClassVar[int]
    ENTRYPOINT_FIELD_NUMBER: _ClassVar[int]
    METADATA_FIELD_NUMBER: _ClassVar[int]
    uri: str
    entrypoint: str
    metadata: _containers.ScalarMap[str, str]
    def __init__(self, uri: _Optional[str] = ..., entrypoint: _Optional[str] = ..., metadata: _Optional[_Mapping[str, str]] = ...) -> None: ...

class Stage(_message.Message):
    __slots__ = ("uri", "processor", "arguments")
    class ArgumentsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: Argument
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[Argument, _Mapping]] = ...) -> None: ...
    URI_FIELD_NUMBER: _ClassVar[int]
    PROCESSOR_FIELD_NUMBER: _ClassVar[int]
    ARGUMENTS_FIELD_NUMBER: _ClassVar[int]
    uri: str
    processor: Processor
    arguments: _containers.MessageMap[str, Argument]
    def __init__(self, uri: _Optional[str] = ..., processor: _Optional[_Union[Processor, _Mapping]] = ..., arguments: _Optional[_Mapping[str, Argument]] = ...) -> None: ...
