import java.util.Date;
import java.util.Map;
import technology.idlab.bridge.*;
import technology.idlab.runner.Processor;

public class Literals extends Processor {
  // Parameters
  private final boolean _bool;
  private final byte _byte;
  private final Date _dateTime;
  private final double _double;
  private final float _float;
  private final int _int;
  private final long _long;
  private final String _string;

  public Literals(Map<String, Object> args) {
    // Call super constructor.
    super(args);

    // Parameters
    this._bool = this.getArgument("_bool");
    this._byte = this.getArgument("_byte");
    this._dateTime = this.getArgument("_dateTime");
    this._double = this.getArgument("_double");
    this._float = this.getArgument("_float");
    this._int = this.getArgument("_int");
    this._long = this.getArgument("_long");
    this._string = this.getArgument("_string");
  }

  public void exec() {}
}
