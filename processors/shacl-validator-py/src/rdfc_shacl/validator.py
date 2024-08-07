import pyshacl
import rdfc
import rdflib


class SHACLValidator(rdfc.Processor):
    # RDFC Channels
    incoming: rdfc.Reader
    report: rdfc.Writer
    outgoing: rdfc.Writer

    # RDF graph containing the SHACL shapes.
    shapes: rdflib.Graph

    def __init__(self, args: rdfc.Arguments):
        # Assign arguments.
        self.incoming = args.reader("incoming")
        self.report = args.writer("report")
        self.outgoing = args.writer("outgoing")

        # Create new graph.
        shapes = args.string("shapes")
        self.shapes = rdflib.Graph()
        self.shapes.parse(shapes, format="text/turtle")

    async def exec(self):
        print("SHACLValidator: Starting execution", flush=True)

        async for message in self.incoming:
            # Create an RDF graph for the incoming data.
            graph = rdflib.Graph()
            graph.parse(data=message, format="text/turtle")

            # Parse using the SHACL validator.
            report: tuple[bool, rdflib.Graph, str] = pyshacl.validate(
                graph, shacl_graph=self.shapes
            )
            conforms, results_graph, results_text = report

            # Pipe into `outgoing` if it conforms, otherwise, write the report to the `report` channel.
            if conforms:
                await self.outgoing.write(message)
            else:
                await self.report.write(results_text.encode())

        # Close outgoing channels after incoming closes.
        await self.outgoing.close()
        await self.report.close()

        print("SHACLValidator: Execution finished", flush=True)
