from src.runtime import Processor
from src.runtime.channel import Channel


class EchoProcessor(Processor):
    incoming: Channel[bytes]
    outgoing: Channel[bytes]

    def __init__(self, args):
        super().__init__(args)

        # Assign arguments.
        self.incoming = args["incoming"]
        self.outgoing = args["outgoing"]

    async def exec(self):
        async for message in self.incoming:
            await self.outgoing.write(message)

        await self.outgoing.close()
