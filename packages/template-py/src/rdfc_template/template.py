import rdfc


class Template(rdfc.Processor):
    # RDFC Channels
    incoming: rdfc.Reader
    outgoing: rdfc.Writer

    def __init__(self, args: rdfc.Arguments):
        # Assign arguments.
        self.incoming = args.reader("incoming")
        self.outgoing = args.writer("outgoing")

    async def exec(self):
        async for message in self.incoming:
            await self.outgoing.write(message)

        await self.outgoing.close()
