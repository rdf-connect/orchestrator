import bridge.Reader;
import java.util.Map;
import runner.Processor;

public class Reporter extends Processor {
    private final Reader reader;

    public Reporter(Map<String, Object> args) {
        // Call super constructor.
        super(args);

        // Parameters
        this.reader = this.getArgument("reader");
    }

    public void exec() {
        while (!reader.isClosed()) {
            Reader.Result result = reader.readSync();

            if (result.isClosed()) {
                break;
            }

            log.info("Received item: " + new String(result.getValue()));
        }
    }
}
