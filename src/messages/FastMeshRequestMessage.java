package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshRequestMessage extends Message {
  private static final long serialVersionUID = 3990510048221123226L;
  public final int TTL;
  public final int MTB;
  
  public FastMeshRequestMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final int timeToLive, final int totalUplinkBandwidth) {
    super(tag, sourceId, destID);
    TTL = timeToLive;
    MTB = totalUplinkBandwidth;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "TTL is: " + Integer.toString(TTL);
  }
}
