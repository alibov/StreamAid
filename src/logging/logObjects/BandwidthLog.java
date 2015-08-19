package logging.logObjects;

import utils.Utils;

public class BandwidthLog extends DataLog {
  private static final long serialVersionUID = 5378026177262962084L;
  public final long totalBandwidth;
  public final long usedBandwidth;
  public final long time;
  public final long bitsInWaitQueue;
  
  public BandwidthLog(final long totalBandwidth, final long usedBandwidth, final long bitsInWaitQueue, final String node) {
    super(node);
    if (bitsInWaitQueue < 0 || usedBandwidth < 0 || totalBandwidth < 0) {
      throw new RuntimeException("can't be lower than zero!");
    }
    this.totalBandwidth = totalBandwidth;
    this.usedBandwidth = usedBandwidth;
    time = Utils.getMovieTime();
    this.bitsInWaitQueue = bitsInWaitQueue;
  }
}
