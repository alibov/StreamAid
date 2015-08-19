package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshGrantMessage extends Message {
  private static final long serialVersionUID = -1134593533031765882L;
  public final long maxDelay;
  
  public FastMeshGrantMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final long delay) {
    super(tag, sourceId, destID);
    maxDelay = delay;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "maxDelay is: " + Long.toString(maxDelay);
  }
}
