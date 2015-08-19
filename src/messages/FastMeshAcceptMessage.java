package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshAcceptMessage extends Message {
  private static final long serialVersionUID = -1134593533031765882L;
  public Long _nodeDelay;
  
  public FastMeshAcceptMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Long nodeDelay) {
    super(tag, sourceId, destID);
    _nodeDelay = nodeDelay;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "";
  }
}
