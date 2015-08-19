package messages;

import experiment.frameworks.NodeAddress;

public class PerformanceInfoMessage extends Message {
  public final int playSeconds;
  public final int chunksPlayed;
  public final double averageLatency;
  public final double CI;
  public final int configurationNumber;

  public PerformanceInfoMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int playSeconds,
      final int chunksPlayed, final double averageLatency, final double CI, final int configurationNumber) {
    super(tag, sourceId, destID);
    this.playSeconds = playSeconds;
    this.chunksPlayed = chunksPlayed;
    this.averageLatency = averageLatency;
    this.CI = CI;
    this.configurationNumber = configurationNumber;
  }

  /**
   *
   */
  private static final long serialVersionUID = 635438279389L;

  @Override protected String getContents() {
    return playSeconds + " " + chunksPlayed + " " + averageLatency + " " + CI + " " + configurationNumber;
  }

  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE * 3 + Double.SIZE * 2;
  }
}
