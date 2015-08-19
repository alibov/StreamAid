package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshLeaveMessage extends Message {
  private static final long serialVersionUID = -1134593533031765882L;
  
  public FastMeshLeaveMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "";
  }
}
