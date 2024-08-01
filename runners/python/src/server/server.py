import asyncio
from typing import Iterator, List, Callable
import sys
import grpc.aio
from grpc.aio import ServicerContext

from src.proto.index_pb2_grpc import RunnerServicer, add_RunnerServicer_to_server
from src.proto.intermediate_pb2 import IRStage
from src.proto.empty_pb2 import Empty
import src.proto.channel_pb2 as GRPCChannel
from src.runtime import Processor
from src.runtime.channel import CallbackChannel, Channel
from src.server.loader import load_processor


class Server(RunnerServicer):
    # Map of a processor URI to their constructor.
    processors: dict[str, Callable[[dict[str, any]], Processor]]

    # The processor instances by their stage URI.
    stages: dict[str, List[Processor]]

    # Messages bound for the orchestrator.
    outgoing_messages: asyncio.Queue[GRPCChannel.ChannelMessage]

    # A map of reader URIs to their concrete instances.
    readers: dict[str, List[Channel[bytes]]]

    def __init__(self):
        print("Starting server.")
        super().__init__()

        # Default values for fields.
        self.stages = {}
        self.outgoing_messages = asyncio.Queue()
        self.readers = {}

    def load(self, stage: IRStage, context: ServicerContext):
        print(f"Loading IRStage: {stage.uri}")

        # Load the processor module.
        class_name = stage.processor.metadata["class_name"]
        processor_constructor = load_processor(stage.processor.entrypoint, class_name)
        self.processors[stage.processor.uri] = processor_constructor

        # TODO: Initialize the arguments.
        args = {}

        # Create the stage.
        processor = processor_constructor(args)
        self.stages.setdefault(stage.uri, []).append(processor)

        # No return value is used.
        return Empty()

    async def exec(self, incoming: Iterator[GRPCChannel.ChannelMessage], context):
        print("Executing pipeline.")

        # Handle incoming messages.
        incoming_message_task = asyncio.create_task(
            self.handle_incoming_messages(incoming)
        )

        # Start all stages.
        executions_task = asyncio.gather(
            *[stage.exec() for (uri, stage) in self.stages]
        )

        # Loop until the `executions_task` is fulfilled.
        while True:
            outgoing_message_task = asyncio.create_task(self.outgoing_messages.get)

            # Await a message, or the fulfillment of the `executions_task`.
            done, pending = await asyncio.wait(
                [executions_task, outgoing_message_task],
                return_when=asyncio.FIRST_COMPLETED,
            )

            # Push the new message to the orchestrator.
            if outgoing_message_task in done:
                yield await outgoing_message_task

            # Signal fulfillment to the orchestrator by returning the procedure call.
            if executions_task in done:
                incoming_message_task.cancel()
                return

    async def handle_incoming_messages(
        self, messages: Iterator[GRPCChannel.ChannelMessage]
    ):
        for message in messages:
            uri = message.channel.uri
            readers = self.readers[uri]

            for reader in readers:
                if message.type == GRPCChannel.ChannelMessageType.DATA:
                    await reader.write(message.data.bytes)
                elif message.type == GRPCChannel.ChannelMessageType.CLOSE:
                    await reader.close()
                else:
                    exit(1)

    def create_reader(self, uri: str) -> Channel[bytes]:
        channel = Channel(uri)
        reader_list = self.readers.setdefault(uri, [])
        reader_list.append(channel)
        return channel

    def create_writer(self, uri: str) -> CallbackChannel[bytes]:
        def on_write(data: bytes) -> None:
            message = GRPCChannel.ChannelMessage(
                GRPCChannel.Channel(uri),
                GRPCChannel.ChannelMessageType.DATA,
                GRPCChannel.ChannelData(data),
            )
            self.outgoing_messages.put(message)

        def on_close() -> None:
            message = GRPCChannel.ChannelMessage(
                GRPCChannel.Channel(uri),
                GRPCChannel.ChannelMessageType.CLOSE,
                GRPCChannel.ChannelData(bytes()),
            )
            self.outgoing_messages.put(message)

        return CallbackChannel(on_write, on_close)

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
