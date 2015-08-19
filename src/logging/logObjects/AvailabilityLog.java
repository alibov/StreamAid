package logging.logObjects;

import utils.Utils;

public class AvailabilityLog extends DataLog {
  public static enum State {
    SERVER, NEW, BUFFERING, PLAYING, LAGGING
  }
  
  final public State state;
  final public long movieTime;
  final public double playbackSpeed;
  final public double played;
  final public double latency;
  
  public AvailabilityLog(final String node, final State state, final double playbackSpeed, final double played, final double latency) {
    super(node);
    this.state = state;
    movieTime = Utils.getMovieTime();
    this.playbackSpeed = playbackSpeed;
    this.played = played;
    this.latency = latency;
  }
  
  private static final long serialVersionUID = 8953700335577594205L;
}
