package messages;

import java.util.List;

import experiment.frameworks.NodeAddress;

public class BTLiveProtInfo extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = -6612428186796675864L;
  public final int totalClubs;
  public final List<Integer> clubsToJoin;
  
  public BTLiveProtInfo(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int totalClubs,
      final List<Integer> clubsToJoin) {
    super(tag, sourceId, destID);
    this.totalClubs = totalClubs;
    this.clubsToJoin = clubsToJoin;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + Integer.SIZE + clubsToJoin.size() * Integer.SIZE;
  }
  
  @Override protected String getContents() {
    return "to join:" + clubsToJoin;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
