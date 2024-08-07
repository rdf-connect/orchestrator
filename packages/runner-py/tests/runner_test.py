import unittest


class RunnerTest(unittest.IsolatedAsyncioTestCase):
    async def test_success(self):
        self.assertEquals(1, 1)
