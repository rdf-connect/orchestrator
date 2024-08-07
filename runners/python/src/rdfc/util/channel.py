import asyncio
import rdfc
import typing


class Channel(rdfc.Reader, rdfc.Writer):
    queue: asyncio.Queue
    closed: bool
    uri: str

    def __init__(self, uri: str):
        self.queue = asyncio.Queue()
        self.closed = False
        self.uri = uri

    def __aiter__(self) -> typing.AsyncIterator[bytes]:
        return self

    async def __anext__(self) -> bytes:
        while True:
            value = await self.queue.get()

            if value is None:
                raise StopAsyncIteration

            return value

    async def write(self, value: bytes) -> None:
        if self.closed:
            exit(1)

        await self.queue.put(value)

    async def close(self) -> None:
        if self.closed is False:
            self.closed = True
            await self.queue.put(None)

    def is_closed(self) -> bool:
        return self.closed
