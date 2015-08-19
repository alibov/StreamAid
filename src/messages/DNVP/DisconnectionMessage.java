package messages.DNVP;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class DisconnectionMessage extends Message {
  private static final long serialVersionUID = -8232388230255187334L;
  public NodeAddress problematicNode;
  public String problematicProtocol;
  
  public DisconnectionMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress problematicNode, final String problematicProtocol) {
    super(tag, sourceId, destID);
    this.problematicNode = problematicNode;
    this.problematicProtocol = problematicProtocol;
  }
  
  @Override protected String getContents() {
    String $ = "";
    $ += "problematic node: " + ((problematicNode != null) ? problematicNode.toString() : "");
    $ += " problematic protocol: " + ((problematicProtocol != null) ? problematicProtocol : "");
    return $;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE + problematicProtocol.getBytes().length * 8;
  }
}
