package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshSendToNeighborsMessage extends Message {
  private static final long serialVersionUID = 8762143296441934136L;
  
  public FastMeshSendToNeighborsMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "";
  }
}
