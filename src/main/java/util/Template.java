package processors;

import java.util.logging.Logger;

import static technology.idlab.logging.LoggerKt.createLogger;

public abstract class Template {
    protected Logger logger = createLogger();

    public Template () {
        logger.info("Creating processor");
    }

    public void setup() {
        logger.info("Setting up processor");
    }

    public void exec() {
        logger.info("Executing processor");
    }
}
