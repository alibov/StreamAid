package messages.OmissionDefense;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class OmissionRequestMessage extends Message {
  private static final long serialVersionUID = -1708645095706615059L;
  public NodeAddress nodeToCheck;
  
  public OmissionRequestMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress nodeToCheck) {
    super(tag, sourceId, destID);
    this.nodeToCheck = nodeToCheck;
  }
  
  @Override protected String getContents() {
    return nodeToCheck.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE;
  }
}
