import java.util.Map;
import technology.idlab.bridge.Writer;
import technology.idlab.runner.Processor;

public class Range extends Processor {
  // Parameters
  private final int start;
  private final int end;
  private final int step;

  // Channels
  private final Writer writer;

  public Range(Map<String, Object> args) {
    // Call super constructor.
    super(args);

    // Parameters
    this.start = this.getArgument("start");
    this.end = this.getArgument("end");
    this.step = this.getArgument("step");

    // Channels
    this.writer = this.getArgument("output");
  }

  public void exec() {
    for (int i = start; i < end; i += step) {
      log.info(Integer.toString(i));
      writer.pushSync(Integer.toString(i).getBytes());
    }
    writer.close();
  }
}
