import rdfc
import typing

OnWrite = typing.Callable[[bytes], typing.Coroutine[typing.Any, typing.Any, None]]
OnClose = typing.Callable[[], typing.Coroutine[typing.Any, typing.Any, None]]


class CallbackChannel(rdfc.Writer):
    closed: bool = False
    on_write: OnWrite
    on_close: OnClose

    def __init__(
        self,
        on_write: OnWrite,
        on_close: OnClose,
    ):
        self.on_write = on_write
        self.on_close = on_close

    async def write(self, value: bytes) -> None:
        await self.on_write(value)

    async def close(self) -> None:
        if self.closed is False:
            self.closed = True
            await self.on_close()

    def is_closed(self) -> bool:
        return self.closed
