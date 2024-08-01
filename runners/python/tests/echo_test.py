import asyncio
import unittest
from pathlib import Path

from src.runtime.channel import Channel
from src.server.loader import load_processor


class TestEcho(unittest.IsolatedAsyncioTestCase):
    async def test(self):
        # Create the processor arguments.
        incoming: Channel[bytes] = Channel("http://example.com/#incoming")
        outgoing: Channel[bytes] = Channel("http://example.com/#outgoing")

        args = {
            "incoming": incoming,
            "outgoing": outgoing,
        }

        # Load processor from module.
        directory = Path(__file__).parent
        path = directory / "processors" / "echo.py"
        constructor = load_processor(str(path), "EchoProcessor")

        # Instantiate with arguments and start execution asynchronously.
        processor = constructor(args)
        execution = asyncio.create_task(processor.exec())
        self.assertFalse(execution.done())

        # Write data to processor and exit.
        await incoming.write(b"Hello, World!")
        await incoming.close()

        # Read data from outgoing channel, should be called just once.
        message_count = 0
        async for data in outgoing:
            self.assertEqual(data, b"Hello, World!")
            message_count += 1
        self.assertTrue(outgoing.is_closed())
        self.assertEqual(message_count, 1)

        self.assertTrue(execution.done())
