package messages;

import java.util.ArrayList;

import experiment.frameworks.NodeAddress;

public class RewardMessage extends Message {
  private static final long serialVersionUID = -4887409310746269686L;
  public final int currentConfNumber;
  public final int playSeconds;
  public final long round;
  public final ArrayList<Double> rewardsList;
  
  public RewardMessage(final String messageTag, final NodeAddress src, final NodeAddress dst, final ArrayList<Double> rewardsList,
      final int currentConfNumber, final int playSeconds, final long round) {
    super(messageTag, src, dst);
    this.playSeconds = playSeconds;
    this.rewardsList = rewardsList;
    this.currentConfNumber = currentConfNumber;
    this.round = round;
  }
  
  @Override protected String getContents() {
    return currentConfNumber + ":" + playSeconds + "s" + " r:" + rewardsList;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE * 2 + Double.SIZE * rewardsList.size() + Long.SIZE;
  }
}
