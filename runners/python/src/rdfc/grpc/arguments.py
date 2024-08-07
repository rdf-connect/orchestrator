from typing import Any

from google.protobuf.internal.containers import MessageMap
import rdfc
from rdfc.proto.intermediate_pb2 import Argument, ArgumentLiteral


class Arguments(rdfc.Arguments):
    arguments: MessageMap[str, Argument]
    repository: rdfc.runtime.ChannelRepository

    def __init__(
        self,
        arguments: MessageMap[str, Argument],
        repository: rdfc.runtime.ChannelRepository,
    ):
        self.arguments = arguments
        self.repository = repository

    def _literal(self, key: str) -> ArgumentLiteral:
        arg = self.arguments.get(key)

        if arg is None:
            raise Exception(f"Argument {key} does not exist.")

        if arg.literal is None:
            raise Exception(f"Argument {key} is not a literal.")

        return arg.literal

    def int(self, key: str) -> int:
        literal = self._literal(key)
        result = literal.int32 or literal.int64 or literal.uint32 or literal.uint64

        if result is None:
            raise Exception(f"Argument {key} is not an integer.")

        return result

    def double(self, key: str) -> float:
        literal = self._literal(key)
        result = literal.float or literal.double

        if result is None:
            raise Exception(f"Argument {key} is not a float.")

        return result

    def string(self, key: str) -> str:
        literal = self._literal(key)
        result = literal.string

        if result is None:
            raise Exception(f"Argument {key} is not a string.")

        return result

    def date(self, key: str) -> Any:
        literal = self._literal(key)
        result = literal.timestamp

        if result is None:
            raise Exception(f"Argument {key} is not a date.")

        return result

    def writer(self, key: str) -> rdfc.Writer:
        literal = self._literal(key)
        result = literal.writer

        if result is None:
            raise Exception(f"Argument {key} is not a writer.")

        return self.repository.create_writer(result.uri)

    def reader(self, key: str) -> rdfc.Reader:
        literal = self._literal(key)
        result = literal.reader

        if result is None:
            raise Exception(f"Argument {key} is not a reader.")

        return self.repository.create_reader(result.uri)
