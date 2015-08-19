package messages.OmissionDefense;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class BlackListMessage extends Message {
  private static final long serialVersionUID = 3017371948011835779L;
  public NodeAddress blamingNode;
  public NodeAddress blamedNode;
  
  public BlackListMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress blamingNode, final NodeAddress blamedNode) {
    super(tag, sourceId, destID);
    this.blamingNode = blamingNode;
    this.blamedNode = blamedNode;
  }
  
  @Override protected String getContents() {
    return "Blaming: " + blamingNode.toString() + " Blamed: " + blamedNode.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + 2 * NodeAddress.SIZE;
  }
}
