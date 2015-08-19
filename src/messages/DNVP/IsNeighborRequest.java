package messages.DNVP;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class IsNeighborRequest extends Message {
  private static final long serialVersionUID = 1194276276780649638L;
  public NodeAddress node;
  
  public IsNeighborRequest(final String tag, final NodeAddress sourceId, final NodeAddress destID,
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
