from abc import ABC, abstractmethod
from typing import AsyncIterator


class Reader(ABC):
    @abstractmethod
    def __aiter__(self) -> AsyncIterator[bytes]:
        raise NotImplementedError()

    @abstractmethod
    async def __anext__(self) -> bytes:
        raise NotImplementedError()

    @abstractmethod
    def is_closed(self) -> bool:
        raise NotImplementedError()
