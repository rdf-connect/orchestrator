from abc import ABC, abstractmethod
from typing import Any
from .writer import Writer
from .reader import Reader


class Arguments(ABC):
    @abstractmethod
    def int(self, key: str) -> int:
        raise NotImplementedError()

    @abstractmethod
    def double(self, key: str) -> float:
        raise NotImplementedError()

    @abstractmethod
    def string(self, key: str) -> str:
        raise NotImplementedError()

    @abstractmethod
    def date(self, key: str) -> Any:
        raise NotImplementedError()

    @abstractmethod
    def writer(self, key: str) -> Writer:
        raise NotImplementedError()

    @abstractmethod
    def reader(self, key: str) -> Reader:
        raise NotImplementedError()
