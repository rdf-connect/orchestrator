import java.util.Optional;
import java.util.Map;

import technology.idlab.runner.Processor;

public class Optionals extends Processor {
    // Parameters
    public final String required;
    public final Optional<String> present;
    public final Optional<String> missing;

    public Optionals(Map<String, Object> args) {
        // Call super constructor.
        super(args);

        // Parameters
        this.required = this.getArgument("required");
        this.present = this.getOptionalArgument("present");
        this.missing = this.getOptionalArgument("missing");
    }

    public void exec() {}
}
