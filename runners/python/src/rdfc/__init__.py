import asyncio

from rdfc.runtime import Reader, Writer, Processor, Arguments


def main():
    async def async_main():
        import rdfc.grpc

        await rdfc.grpc.Server.launch()

    asyncio.run(async_main())


__all__ = [
    "Reader",
    "Writer",
    "Processor",
    "Arguments",
]
