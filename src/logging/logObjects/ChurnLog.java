package logging.logObjects;

import utils.Common;
import utils.Utils;

public class ChurnLog extends DataLog {
  public final boolean joined;
  public final long time;
  public final int alg;
  public final long startupBuffer;
  public final long cycle;
  public final boolean bufferFromFirstChunk;
  
  public ChurnLog(final String node, final boolean joined, final int startupBuffer, final boolean bufferFromFirstChunk) {
    super(node);
    this.joined = joined;
    time = Utils.getTime();
    alg = Common.currentConfiguration.getNodeGroup(node);
    cycle = Common.currentConfiguration.cycleLength;
    this.startupBuffer = startupBuffer;
    this.bufferFromFirstChunk = bufferFromFirstChunk;
  }
  
  private static final long serialVersionUID = -4726173217118305879L;
}
