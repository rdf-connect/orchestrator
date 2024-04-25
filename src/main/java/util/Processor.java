package util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Logger;

import static technology.idlab.logging.LoggerKt.createLogger;

public abstract class Template {
    /**
     * Processors which wish to log messages should use the logger provided by
     * the template class. This logger is created with the name of the class
     * which extends the template.
     */
    protected final Logger logger = createLogger();

    /**
     * The arguments of a processor are stored in a map and can be accessed by
     * name. At the time of writing, the user must manually cast the arguments
     * to the correct type.
     */
    private final Map<String, Object> arguments = new HashMap<>();

    public Template(Map<String, Object> arguments) {
        this.arguments.putAll(arguments);
    }

    public Template() {}

    protected <T> T getArgument(String name) {
        Object result = arguments.get(name);
        return (T) result;
    }

    protected <T> Optional<T> getOptionalArgument(String name) {
        return Optional.ofNullable(getArgument(name));
    }

    public void setup() {
        logger.info("Setting up processor");
    }

    public void exec() {
        logger.info("Executing processor");
    }
}
