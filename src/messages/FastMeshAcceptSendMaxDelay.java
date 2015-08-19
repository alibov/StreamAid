package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshAcceptSendMaxDelay extends Message {
  public final long maxDelay;
  
  public FastMeshAcceptSendMaxDelay(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final long maxDelayFromSource) {
    super(tag, sourceId, destID);
    maxDelay = maxDelayFromSource;
  }
  
  /**
   * 
   */
  private static final long serialVersionUID = 7675348643854L;
  
  @Override protected String getContents() {
    // TODO Auto-generated method stub
    return "";
  }
  
  @Override public boolean isOverheadMessage() {
    // TODO Auto-generated method stub
    return true;
  }
}
