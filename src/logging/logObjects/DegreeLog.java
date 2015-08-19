package logging.logObjects;

public class DegreeLog extends DataLog {
  private static final long serialVersionUID = 4401007565698454497L;
  public final int degree;
  public final long duration;
  public final String protocol;
  
  public DegreeLog(final String node, final String protocol, final int degree, final long duration) {
    super(node);
    if (duration < 0 || degree < 0) {
      throw new IllegalArgumentException();
    }
    this.protocol = protocol;
    this.degree = degree;
    this.duration = duration;
  }
}
