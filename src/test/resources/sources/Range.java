import io.reactivex.rxjava3.subjects.PublishSubject;
import java.util.Map;
import runner.Processor;

public class Range extends Processor {
    // Parameters
    private final int start;
    private final int end;
    private final int step;

    // Channels
    private final PublishSubject<String> outgoing;

    public Range(Map<String, Object> args) {
        // Call super constructor.
        super(args);

        // Parameters
        this.start = this.getArgument("start");
        this.end = this.getArgument("end");
        this.step = this.getArgument("step");

        // Channels
        this.outgoing = this.getArgument("outgoing");
    }

    public void setup() {
        logger.info("Binding to outgoing channel.");
    }

    public void exec() {
        logger.info("Initializing emitting loop.");

        for (int i = start; i < end; i += step) {
            logger.info("Emitting " + i);
            outgoing.onNext(Integer.toString(i));
        }

        logger.info("Closing outgoing channel.");
        outgoing.onComplete();
    }
}
