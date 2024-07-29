from typing import Iterator

import grpc.aio
from grpc.aio import ServicerContext

from src.proto.index_pb2_grpc import RunnerServicer, add_RunnerServicer_to_server
from src.proto.intermediate_pb2 import IRStage
from src.proto.empty_pb2 import Empty
from src.proto.channel_pb2 import ChannelMessage


class Server(RunnerServicer):
    def __init__(self):
        super().__init__()
        print("Starting server.")

    def load(self, stage: IRStage, context: ServicerContext):
        print(f"Loading IRStage: {stage.uri}")
        return Empty()

    def exec(self, incoming: Iterator[ChannelMessage], context):
        print("Executing pipeline.")
        for message in incoming:
            print(f"Incoming data: {message.channel.uri}")

            # Function as an echo server and return the data verbatim.
            yield incoming

    @staticmethod
    async def launch():
        server = grpc.aio.server()
        add_RunnerServicer_to_server(Server(), server)
        server.add_insecure_port("[::]:50051")
        await server.start()
        await server.wait_for_termination()
