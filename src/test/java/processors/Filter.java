package processors;

import java.util.List;
import java.util.Map;
import technology.idlab.bridge.*;
import technology.idlab.runner.Processor;
import technology.idlab.runner.ProcessorDefinition;

@ProcessorDefinition(resource = "/processors/filter.ttl")
public class Filter extends Processor {
  // Parameters
  private final List<Integer> whitelist;

  // Channels
  private final Reader reader;
  private final Writer writer;

  public Filter(Map<String, Object> args) {
    // Call super constructor.
    super(args);

    // Parameters
    this.whitelist = this.getArgument("whitelist");

    // Channels
    this.reader = this.getArgument("input");
    this.writer = this.getArgument("output");
  }

  public void exec() {
    while (true) {
      Reader.Result data = reader.readSync();

      if (data.isClosed()) {
        break;
      }

      byte[] input = data.getValue();
      Integer as_int = Integer.parseInt(new String(input));

      if (!whitelist.contains(as_int)) {
        log.info("Blocked value " + as_int);
        continue;
      }

      log.info("Allowed value " + as_int);
      writer.pushSync(input);
    }
    writer.close();
  }
}
