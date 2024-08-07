from abc import ABC, abstractmethod
from .reader import Reader
from .writer import Writer


class ChannelRepository(ABC):
    @abstractmethod
    def create_reader(self, uri: str) -> Reader:
        raise NotImplementedError()

    @abstractmethod
    def create_writer(self, uri: str) -> Writer:
        raise NotImplementedError()
