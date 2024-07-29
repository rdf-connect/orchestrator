from typing import Any, Mapping


class Processor:
    args: Mapping[str, Any]

    def __init__(self, args: Mapping[str, Any]):
        self.args = args

    async def exec(self):
        NotImplementedError()
