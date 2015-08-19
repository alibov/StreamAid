package messages.OmissionDefense;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class SourceAccusationMessage extends Message {
  private static final long serialVersionUID = -2934091673773550715L;
  public NodeAddress blamedNode;
  
  public SourceAccusationMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final NodeAddress blamedNode) {
    super(tag, sourceId, destID);
    this.blamedNode = blamedNode;
  }
  
  @Override protected String getContents() {
    return "Blamed node: " + blamedNode.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE;
  }
}
