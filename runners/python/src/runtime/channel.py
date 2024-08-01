import asyncio
from typing import TypeVar, Generic, AsyncIterator, Callable

T = TypeVar("T")


class Channel(Generic[T]):
    queue: asyncio.Queue
    closed: bool
    uri: str

    def __init__(self, uri: str):
        self.queue = asyncio.Queue()
        self.closed = False
        self.uri = uri

    def __aiter__(self) -> AsyncIterator[T]:
        return self

    async def __anext__(self) -> T:
        while True:
            value = await self.queue.get()

            if value is None:
                raise StopAsyncIteration

            return value

    async def write(self, value: T) -> None:
        if self.closed:
            exit(1)

        await self.queue.put(value)

    async def close(self) -> None:
        if self.closed is False:
            self.closed = True
            await self.queue.put(None)

    def is_closed(self) -> bool:
        return self.closed


class CallbackChannel(Generic[T]):
    closed: bool = False
    on_write: Callable[[T], None]
    on_close: Callable[[], None]

    def __init__(self, on_write: Callable[[T], None], on_close: Callable[[], None]):
        self.on_write = on_write
        self.on_close = on_close

    async def write(self, value: T) -> None:
        self.on_write(value)

    def close(self) -> None:
        if self.closed is False:
            self.closed = True
            self.on_close()

    def is_closed(self) -> bool:
        return self.closed
