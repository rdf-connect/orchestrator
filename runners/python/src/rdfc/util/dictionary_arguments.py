from datetime import date
from typing import Mapping, Any

import rdfc
from rdfc import Reader, Writer


class DictionaryArguments(rdfc.Arguments):
    arguments: Mapping[str, Any]

    def __init__(self, arguments: Mapping[str, Any]):
        self.arguments = arguments

    def _get(self, key: str) -> Any:
        result = self.arguments.get(key)

        if result is None:
            raise Exception(f"Argument '{key}' is not set.")

        return result

    def int(self, key: str) -> int:
        result = self._get(key)

        if not isinstance(result, int):
            raise Exception(f"Argument '{key}' is not an instance of 'int'")

        return result

    def double(self, key: str) -> float:
        result = self._get(key)

        if not isinstance(result, float):
            raise Exception(f"Argument '{key}' is not an instance of 'float'")

        return result

    def string(self, key: str) -> str:
        result = self._get(key)

        if not isinstance(result, str):
            raise Exception(f"Argument '{key}' is not an instance of 'str'")

        return result

    def date(self, key: str) -> date:
        result = self._get(key)

        if not isinstance(result, date):
            raise Exception(f"Argument '{key}' is not an instance of 'date'")

        return result

    def writer(self, key: str) -> Writer:
        result = self._get(key)

        if not isinstance(result, Writer):
            raise Exception(f"Argument '{key}' is not an instance of 'Writer'")

        return result

    def reader(self, key: str) -> Reader:
        result = self._get(key)

        if not isinstance(result, Reader):
            raise Exception(f"Argument '{key}' is not an instance of 'Reader'")

        return result
