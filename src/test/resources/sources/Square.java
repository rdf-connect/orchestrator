import java.util.Map;
import technology.idlab.bridge.Reader;
import technology.idlab.bridge.Writer;
import technology.idlab.runner.Processor;

public class Square extends Processor {
  // Channels
  private final Reader reader;
  private final Writer writer;

  public Square(Map<String, Object> args) {
    // Call super constructor.
    super(args);

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

      int value = Integer.parseInt(new String(data.getValue()));
      int square = value * value;
      byte[] result = Integer.toString(square).getBytes();

      log.info(value + " * " + value + " = " + square);
      writer.pushSync(result);
    }
    writer.close();
  }
}
