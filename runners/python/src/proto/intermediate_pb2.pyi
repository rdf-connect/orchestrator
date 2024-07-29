from google.protobuf.internal import containers as _containers
from google.protobuf.internal import enum_type_wrapper as _enum_type_wrapper
from google.protobuf import descriptor as _descriptor
from google.protobuf import message as _message
from typing import ClassVar as _ClassVar, Iterable as _Iterable, Mapping as _Mapping, Optional as _Optional, Union as _Union

DESCRIPTOR: _descriptor.FileDescriptor

class IRParameterType(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    BOOLEAN: _ClassVar[IRParameterType]
    BYTE: _ClassVar[IRParameterType]
    DATE: _ClassVar[IRParameterType]
    DOUBLE: _ClassVar[IRParameterType]
    FLOAT: _ClassVar[IRParameterType]
    INT: _ClassVar[IRParameterType]
    LONG: _ClassVar[IRParameterType]
    STRING: _ClassVar[IRParameterType]
    WRITER: _ClassVar[IRParameterType]
    READER: _ClassVar[IRParameterType]

class IRParameterPresence(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    OPTIONAL: _ClassVar[IRParameterPresence]
    REQUIRED: _ClassVar[IRParameterPresence]

class IRParameterCount(int, metaclass=_enum_type_wrapper.EnumTypeWrapper):
    __slots__ = ()
    SINGLE: _ClassVar[IRParameterCount]
    LIST: _ClassVar[IRParameterCount]
BOOLEAN: IRParameterType
BYTE: IRParameterType
DATE: IRParameterType
DOUBLE: IRParameterType
FLOAT: IRParameterType
INT: IRParameterType
LONG: IRParameterType
STRING: IRParameterType
WRITER: IRParameterType
READER: IRParameterType
OPTIONAL: IRParameterPresence
REQUIRED: IRParameterPresence
SINGLE: IRParameterCount
LIST: IRParameterCount

class IRParameters(_message.Message):
    __slots__ = ("parameters",)
    class ParametersEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: IRParameter
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[IRParameter, _Mapping]] = ...) -> None: ...
    PARAMETERS_FIELD_NUMBER: _ClassVar[int]
    parameters: _containers.MessageMap[str, IRParameter]
    def __init__(self, parameters: _Optional[_Mapping[str, IRParameter]] = ...) -> None: ...

class IRParameter(_message.Message):
    __slots__ = ("simple", "complex", "presence", "count")
    SIMPLE_FIELD_NUMBER: _ClassVar[int]
    COMPLEX_FIELD_NUMBER: _ClassVar[int]
    PRESENCE_FIELD_NUMBER: _ClassVar[int]
    COUNT_FIELD_NUMBER: _ClassVar[int]
    simple: IRParameterType
    complex: IRParameters
    presence: IRParameterPresence
    count: IRParameterCount
    def __init__(self, simple: _Optional[_Union[IRParameterType, str]] = ..., complex: _Optional[_Union[IRParameters, _Mapping]] = ..., presence: _Optional[_Union[IRParameterPresence, str]] = ..., count: _Optional[_Union[IRParameterCount, str]] = ...) -> None: ...

class IRProcessor(_message.Message):
    __slots__ = ("uri", "entrypoint", "parameters", "metadata")
    class ParametersEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: IRParameter
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[IRParameter, _Mapping]] = ...) -> None: ...
    class MetadataEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: str
        def __init__(self, key: _Optional[str] = ..., value: _Optional[str] = ...) -> None: ...
    URI_FIELD_NUMBER: _ClassVar[int]
    ENTRYPOINT_FIELD_NUMBER: _ClassVar[int]
    PARAMETERS_FIELD_NUMBER: _ClassVar[int]
    METADATA_FIELD_NUMBER: _ClassVar[int]
    uri: str
    entrypoint: str
    parameters: _containers.MessageMap[str, IRParameter]
    metadata: _containers.ScalarMap[str, str]
    def __init__(self, uri: _Optional[str] = ..., entrypoint: _Optional[str] = ..., parameters: _Optional[_Mapping[str, IRParameter]] = ..., metadata: _Optional[_Mapping[str, str]] = ...) -> None: ...

class IRArgumentSimple(_message.Message):
    __slots__ = ("value",)
    VALUE_FIELD_NUMBER: _ClassVar[int]
    value: _containers.RepeatedScalarFieldContainer[str]
    def __init__(self, value: _Optional[_Iterable[str]] = ...) -> None: ...

class IRArgumentMap(_message.Message):
    __slots__ = ("arguments",)
    class ArgumentsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: IRArgument
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[IRArgument, _Mapping]] = ...) -> None: ...
    ARGUMENTS_FIELD_NUMBER: _ClassVar[int]
    arguments: _containers.MessageMap[str, IRArgument]
    def __init__(self, arguments: _Optional[_Mapping[str, IRArgument]] = ...) -> None: ...

class IRArgumentComplex(_message.Message):
    __slots__ = ("value",)
    VALUE_FIELD_NUMBER: _ClassVar[int]
    value: _containers.RepeatedCompositeFieldContainer[IRArgumentMap]
    def __init__(self, value: _Optional[_Iterable[_Union[IRArgumentMap, _Mapping]]] = ...) -> None: ...

class IRArgument(_message.Message):
    __slots__ = ("simple", "complex")
    SIMPLE_FIELD_NUMBER: _ClassVar[int]
    COMPLEX_FIELD_NUMBER: _ClassVar[int]
    simple: IRArgumentSimple
    complex: IRArgumentComplex
    def __init__(self, simple: _Optional[_Union[IRArgumentSimple, _Mapping]] = ..., complex: _Optional[_Union[IRArgumentComplex, _Mapping]] = ...) -> None: ...

class IRStage(_message.Message):
    __slots__ = ("uri", "processor", "arguments")
    class ArgumentsEntry(_message.Message):
        __slots__ = ("key", "value")
        KEY_FIELD_NUMBER: _ClassVar[int]
        VALUE_FIELD_NUMBER: _ClassVar[int]
        key: str
        value: IRArgument
        def __init__(self, key: _Optional[str] = ..., value: _Optional[_Union[IRArgument, _Mapping]] = ...) -> None: ...
    URI_FIELD_NUMBER: _ClassVar[int]
    PROCESSOR_FIELD_NUMBER: _ClassVar[int]
    ARGUMENTS_FIELD_NUMBER: _ClassVar[int]
    uri: str
    processor: IRProcessor
    arguments: _containers.MessageMap[str, IRArgument]
    def __init__(self, uri: _Optional[str] = ..., processor: _Optional[_Union[IRProcessor, _Mapping]] = ..., arguments: _Optional[_Mapping[str, IRArgument]] = ...) -> None: ...
