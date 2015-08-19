package messages;

import experiment.frameworks.NodeAddress;

public class ConnectionRequestForwardMessage extends Message {
  private static final long serialVersionUID = 7626973727098801152L;
  public NodeAddress newNode;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE;
  }
  
  public ConnectionRequestForwardMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final NodeAddress newNode) {
    super(tag, sourceId, destID);
    this.newNode = newNode;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public void updateSourceID(final NodeAddress newSource) {
    if (sourceId.equals(newNode)) {
      newNode = newSource;
    }
    super.updateSourceID(newSource);
  }
  
  @Override protected String getContents() {
    return newNode.toString();
  }
}
