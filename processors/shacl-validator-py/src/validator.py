from typing import Any
from rdflib import Graph
from pyshacl import validate
from rdfc.src.runtime.channel import Channel
from rdfc.src.runtime import Processor


class SHACLValidator(Processor):
    # RDFC Channels
    incoming: Channel[bytes]
    outgoing: Channel[bytes]
    report: Channel[bytes]

    # RDF graph containing the SHACL shapes.
    shapes: Graph = Graph()

    def __init__(self, args: dict[str, Any]):
        super().__init__(args)

        # Assign arguments.
        self.incoming = args["incoming"]
        self.report = args["report"]
        self.outgoing = args["outgoing"]
        self.shapes.parse(args["shapes"], format="text/turtle")

    async def exec(self):
        async for message in self.incoming:
            # Create an RDF graph for the incoming data.
            graph = Graph()
            graph.parse(data=message, format="text/turtle")

            # Parse using the SHACL validator.
            report: tuple[bool, Graph, str] = validate(graph, shacl_graph=self.shapes)
            conforms, results_graph, results_text = report

            # Pipe into `outgoing` if it conforms, otherwise, write the report to the `report` channel.
            if conforms:
                await self.outgoing.write(message)
            else:
                await self.report.write(results_text.encode())

        # Close outgoing channels after incoming closes.
        await self.outgoing.close()
        await self.report.close()
