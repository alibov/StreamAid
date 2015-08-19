package messages.DNVP;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class IsNeighborResponse extends Message {
  private static final long serialVersionUID = -4277672582130808053L;
  public NodeAddress node;
  
  public IsNeighborResponse(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress node) {
    super(tag, sourceId, destID);
    this.node = node;
  }
  
  @Override protected String getContents() {
    return node.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE;
  }
}
