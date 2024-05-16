import bridge.Reader;
import bridge.Writer;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import technology.idlab.logging.Log;
import technology.idlab.runner.Processor;

public class Filter extends Processor {
    // Parameters
    private final Set<Integer> whitelist = new HashSet<>();

    // Channels
    private final Reader reader;
    private final Writer writer;

    public Filter(Map<String, Object> args) {
        // Call super constructor.
        super(args);

        // Parameters
        int[] whitelist = this.getArgument("whitelist");
        for (int i = 0; i < whitelist.length; i++) {
            log.info("Adding " + whitelist[i] + " to whitelist");
            this.whitelist.add(i);
        }

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
