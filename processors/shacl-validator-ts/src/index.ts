import { Arguments, Processor } from "jvm-runner-ts";
import rdf, { PrefixMapFactory } from "rdf-ext";
import Serializer from "@rdfjs/serializer-turtle";
import formatsPretty from "@rdfjs/formats/pretty.js";
import { Validator } from "shacl-engine";
import { Readable } from "stream";

export default class SHACLValidator extends Processor {
  private incoming = this.args.get("input", {
    type: "reader",
    list: "false",
    nullable: "false",
  });

  private outgoing = this.args.get("outgoing", {
    type: "writer",
    list: "false",
    nullable: "false",
  });

  private report = this.args.get("report", {
    type: "writer",
    list: "false",
    nullable: "true",
  });

  private path = this.args.get("path", {
    type: "string",
    list: "false",
    nullable: "false",
  });

  private mime =
    this.args.get("mime", {
      type: "string",
      list: "false",
      nullable: "true",
    }) ?? "text/turtle";

  private validationIsFatal =
    this.args.get("validation_is_fatal", {
      type: "boolean",
      list: "false",
      nullable: "true",
    }) ?? false;

  constructor(args: Arguments) {
    super(args);

    // Use pretty formatting.
    rdf.formats.import(formatsPretty);
  }

  async exec() {
    // Create a new serializer for the SHACL reports.
    const prefixes = new PrefixMapFactory().prefixMap();
    prefixes.set("sh", rdf.namedNode("http://www.w3.org/ns/shacl#"));
    const serializer = new Serializer({ prefixes });

    // Create a new parser for the incoming data.
    const parser = rdf.formats.parsers.get(this.mime);
    if (!parser) {
      throw Error("Could not initialize parser");
    }

    // Create a new validator.
    const res = await rdf.fetch(this.path);
    if (!res.ok) {
      throw Error("Could not parse SHACL path.");
    }

    const shapes = await res.dataset().catch(() => {
      throw Error("Could not parse SHACL file.");
    });

    // Parse input stream using shape stream.
    // @ts-expect-error Factory is valid.
    const validator = new Validator(shapes, { factory: rdf });

    // eslint-ignore no-constant-condition
    while (true) {
      // Parse data into a dataset.
      const data = await this.incoming.read();
      const rawStream = Readable.from(data);
      const quadStream = parser.import(rawStream);
      const dataset = await rdf
        .dataset()
        .import(quadStream)
        .catch(() => {
          throw new Error("The incoming data could not be parsed");
        });

      // Run through validator.
      const result = await validator.validate({ dataset });

      // Pass through data if valid.
      if (result.conforms) {
        this.outgoing.write(data);
      } else if (this.validationIsFatal) {
        throw new Error("Validation failed");
      } else if (this.report) {
        const resultRaw = serializer.transform(result.dataset);
        this.report.write(new TextEncoder().encode(resultRaw));
      }
    }
  }
}
