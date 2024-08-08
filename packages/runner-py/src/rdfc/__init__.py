import asyncio
import pathlib
import sys
from rdfc.runtime import Reader, Writer, Processor, Arguments


def main():
    src_file = pathlib.Path(__file__).resolve()
    src_dir = src_file.parent
    proto_dir = f"{src_dir}/proto"

    sys.path.append(proto_dir)

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
