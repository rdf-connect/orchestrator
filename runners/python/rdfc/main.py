from src.server import Server
import asyncio


def main():
    asyncio.run(async_main())


async def async_main():
    await Server.launch()
