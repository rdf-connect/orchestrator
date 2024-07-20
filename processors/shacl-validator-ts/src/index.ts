import { Arguments, Log, Processor } from "jvm-runner-ts";
import rdf, { PrefixMapFactory } from "rdf-ext";
import Serializer from "@rdfjs/serializer-turtle";
import formatsPretty from "@rdfjs/formats/pretty.js";
import { Validator } from "shacl-engine";
import { Readable } from "stream";

export default class SHACLValidator extends Processor {
  private incoming = this.args.get("incoming", {
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

  private path = this.args.get("shapes", {
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
      Log.shared.fatal("Could not fetch SHACL file.");
    }

    // Read the shapes file.
    const shapes = await res.dataset().catch((e) => {
      Log.shared.fatal(`Could not parse SHACL file: ${e}`);
    });

    // Parse input stream using shape stream.
    // @ts-expect-error Factory is valid.
    const validator = new Validator(shapes, { factory: rdf });

    // eslint-ignore no-constant-condition
    while (!this.incoming.isClosed()) {
      // Convert incoming data to a quad stream.
      const data = await this.incoming.read();
      const rawStream = Readable.from(data);
      const quadStream = parser.import(rawStream);

      // Create a new dataset.
      const dataset = await rdf
        .dataset()
        .import(quadStream)
        .catch(() => {
          Log.shared.fatal("The incoming data could not be parsed");
        });

      // Run through validator.
      const result = await validator.validate({ dataset });

      // Pass through data if valid.
      if (result.conforms) {
        Log.shared.debug("Validation passed.");
        this.outgoing.write(data);
      } else if (this.validationIsFatal) {
        Log.shared.fatal("Validation failed and is fatal.");
      } else if (this.report) {
        Log.shared.debug("Validation failed.");
        const resultRaw = serializer.transform(result.dataset);
        this.report.write(new TextEncoder().encode(resultRaw));
      }
    }

    this.outgoing.close();
    this.report?.close();
  }
}
