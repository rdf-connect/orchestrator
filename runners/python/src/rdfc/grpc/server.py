import asyncio
from typing import Iterator, List, Callable, AsyncGenerator, AsyncIterator, Mapping

from google.protobuf.empty_pb2 import Empty
import grpc
import sys
import rdfc
import rdfc.util
import rdfc.runtime
from rdfc.proto.channel_pb2 import (
    Channel,
    ChannelData,
    ChannelMessageType,
    ChannelMessage,
)
from .arguments import Arguments
from ..proto.index_pb2_grpc import RunnerServicer, add_RunnerServicer_to_server

from ..proto.intermediate_pb2 import Stage


class Server(RunnerServicer, rdfc.runtime.ChannelRepository):
    # Map of a processor URI to their constructor.
    processors: dict[str, Callable[[rdfc.Arguments], rdfc.Processor]]

    # The processor instances by their stage URI.
    stages: dict[str, rdfc.Processor]

    # Messages bound for the orchestrator.
    outgoing_messages: asyncio.Queue[ChannelMessage]

    # A map of reader URIs to their concrete instances.
    readers: dict[str, List[rdfc.Writer]]

    def __init__(self):
        print("Starting server.")
        super().__init__()

        # Default values for fields.
        self.processors = dict()
        self.stages = dict()
        self.outgoing_messages = asyncio.Queue()
        self.readers = dict()

    def load(self, stage: Stage, context: grpc.ServicerContext):
        print(f"Loading stage: {stage.uri}")

        # The class name needs to be given, since we don't know what declaration to load from a source file otherwise.
        class_name = stage.processor.metadata.get("class_name", default=None)
        if class_name is None:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(
                "Processor does not include the `class_name` metadata field."
            )

            return Empty()

        # The class name needs to be given, since we don't know what declaration to load from a source file otherwise.
        module_name = stage.processor.metadata.get("module_name", default=None)
        if module_name is None:
            context.set_code(grpc.StatusCode.INVALID_ARGUMENT)
            context.set_details(
                "Processor does not include the `module_name` metadata field."
            )
            return Empty()

        # Load processor module into the runtime.
        try:
            constructor = rdfc.util.Wheel.load(
                stage.processor.entrypoint, module_name, class_name
            )
        except Exception as exception:
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(
                f"Processor constructor could not be loaded into runtime: {exception}"
            )
            return Empty()
        self.processors[stage.processor.uri] = constructor

        # Initialize the arguments.
        args = Arguments(stage.arguments, self)

        # Create the stage.
        try:
            processor = constructor(args)
        except Exception as exception:
            context.set_code(grpc.StatusCode.INTERNAL)
            context.set_details(f"Processor could not be instantiated: {exception}")
            return Empty()
        self.stages[stage.uri] = processor

        # No return value is used.
        return Empty()

    async def exec(
        self, incoming: AsyncIterator[ChannelMessage], context
    ) -> AsyncGenerator:
        print("Executing pipeline.", flush=True)

        async def handle_incoming_messages(
            messages: AsyncIterator[ChannelMessage],
        ):
            print("Begin handling incoming messages.", flush=True)

            async for message in messages:
                uri = message.channel.uri
                readers = self.readers[uri]

                for reader in readers:
                    if message.type == ChannelMessageType.DATA:
                        await reader.write(message.data.bytes)
                    elif message.type == ChannelMessageType.CLOSE:
                        await reader.close()
                    else:
                        exit(1)

            print("Stop handling incoming messages.", flush=True)

        # Handle incoming messages.
        incoming_message_task = asyncio.create_task(handle_incoming_messages(incoming))

        # Start all stages.
        print("Starting executions", flush=True)
        executions_task = asyncio.gather(
            *[stage.exec() for (uri, stage) in self.stages.items()]
        )

        # Loop until the `executions_task` is fulfilled.
        while True:
            outgoing_message_task = asyncio.create_task(self.outgoing_messages.get())

            # Await a message, or the fulfillment of the `executions_task`.
            done, pending = await asyncio.wait(
                [executions_task, outgoing_message_task],
                return_when=asyncio.FIRST_COMPLETED,
            )

            # Push the new message to the orchestrator.
            if outgoing_message_task in done:
                print("Pushing new message", flush=True)
                yield await outgoing_message_task

            # Signal fulfillment to the orchestrator by returning the procedure call.
            elif executions_task in done:
                print("Executions finished", flush=True)
                incoming_message_task.cancel()
                return

    def create_reader(self, uri: str) -> rdfc.util.Channel:
        channel = rdfc.util.Channel(uri)
        reader_list = self.readers.setdefault(uri, [])
        reader_list.append(channel)
        return channel

    def create_writer(self, uri: str) -> rdfc.util.CallbackChannel:
        async def on_write(data: bytes) -> None:
            msg = ChannelMessage()
            msg.channel.uri = uri
            msg.type = ChannelMessageType.DATA
            msg.data.bytes = data
            await self.outgoing_messages.put(msg)

        async def on_close() -> None:
            msg = ChannelMessage()
            msg.channel.uri = uri
            msg.type = ChannelMessageType.CLOSE
            msg.data.bytes = bytes()
            await self.outgoing_messages.put(msg)

        return rdfc.util.CallbackChannel(on_write, on_close)

    @staticmethod
    async def launch():
        hostname = sys.argv[1]
        port = sys.argv[2]
        print(f"Binding to grpc://{hostname}:{port}")

        server = grpc.aio.server()
        add_RunnerServicer_to_server(Server(), server)
        server.add_insecure_port(f"{hostname}:{port}")
        await server.start()
        await server.wait_for_termination()
