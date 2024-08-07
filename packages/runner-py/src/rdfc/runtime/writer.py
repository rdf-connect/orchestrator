from abc import ABC, abstractmethod


class Writer(ABC):
    @abstractmethod
    async def write(self, value: bytes) -> None:
        raise NotImplementedError()

    @abstractmethod
    async def close(self) -> None:
        raise NotImplementedError()

    @abstractmethod
    def is_closed(self) -> bool:
        raise NotImplementedError()
