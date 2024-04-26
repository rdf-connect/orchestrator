import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import java.util.Map;
import runner.Processor;

public class Reporter extends Processor {
    private final Observable<String> incoming;

    public Reporter(Map<String, Object> args) {
        // Call super constructor.
        super(args);

        // Parameters
        this.incoming = this.getArgument("incoming");
    }

    public void setup() {
        // Local variables
        Disposable disposable = incoming.subscribe(
            item -> log.info("Received item: " + item),
            error -> log.severe("Error: " + error),
            () -> log.info("Channel closed.")
        );
    }
}
