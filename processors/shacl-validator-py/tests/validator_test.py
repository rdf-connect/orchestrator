import os
import unittest
import rdfc
import rdfc.util

from src.rdfc_shacl import SHACLValidator


class ValidatorTest(unittest.IsolatedAsyncioTestCase):
    async def test_success(self):
        # Create channels.
        incoming = rdfc.util.Channel("incoming")
        report = rdfc.util.Channel("report")
        outgoing = rdfc.util.Channel("outgoing")

        valid_path = os.path.join(os.path.dirname(__file__), "resources/valid.ttl")
        invalid_path = os.path.join(os.path.dirname(__file__), "resources/invalid.ttl")
        shapes_path = os.path.join(os.path.dirname(__file__), "resources/shapes.ttl")

        f = open(valid_path, "r")
        valid = f.read().encode()
        f.close()

        f = open(invalid_path, "r")
        invalid = f.read().encode()
        f.close()

        # Create argument dictionary.
        args = {
            "incoming": incoming,
            "report": report,
            "outgoing": outgoing,
            "shapes": shapes_path,
        }

        # Initialize processor.
        processor = SHACLValidator(rdfc.util.DictionaryArguments(args))

        # Write a valid and invalid message into the processor.
        await incoming.write(valid)
        await incoming.write(invalid)
        await incoming.close()

        # Execute the pipeline.
        await processor.exec()

        # Gather valid messages.
        data = [message async for message in outgoing]
        self.assertEqual(1, len(data))

        # Check contents.
        self.assertEqual(valid, data[0])

        # Gather invalid messages.
        data = [message async for message in report]
        self.assertEqual(1, len(data))

        # Check contents.
        self.assertTrue(b"Conforms: False" in data[0])
