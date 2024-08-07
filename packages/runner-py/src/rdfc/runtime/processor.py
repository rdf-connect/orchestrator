from abc import ABC, abstractmethod
from .arguments import Arguments


class Processor(ABC):
    @abstractmethod
    def __init__(self, args: Arguments):
        raise NotImplementedError()

    @abstractmethod
    async def exec(self):
        raise NotImplementedError()
