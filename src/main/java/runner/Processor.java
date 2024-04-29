package runner;

import technology.idlab.logging.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public abstract class Processor {
    /**
     * Processors which wish to log messages should use the logger provided by
     * the template class. This logger is created with the name of the class
     * which extends the template.
     */
    protected final Log log = Log.Companion.getShared();

    /**
     * The arguments of a processor are stored in a map and can be accessed by
     * name. At the time of writing, the user must manually cast the arguments
     * to the correct type.
     */
    private final Map<String, Object> arguments;

    public Processor(Map<String, Object> arguments) {
        this.arguments = arguments;
    }

    public Processor() {
        this.arguments = new HashMap<>();
    }

    protected <T> T getArgument(String name) {
        Log.Companion.getShared().debug(name);

        Object result = arguments.get(name);

        if (result == null) {
            Log.Companion.getShared().fatal("Argument " + name + " is missing.");
        }

        return (T) result;
    }

    protected <T> Optional<T> getOptionalArgument(String name) {
        Log.Companion.getShared().debug(name + " (optional)");
        return Optional.ofNullable(getArgument(name));
    }

    public void setup() {}

    public void exec() {}
}
