package messages.OmissionDefense;

import messages.Message;
import entites.DNVP.DNVPAuthorizationApproval;
import experiment.frameworks.NodeAddress;

public class AccusationMessage extends Message {
  private static final long serialVersionUID = 7306198281455982993L;
  public NodeAddress blamedNode;
  public DNVPAuthorizationApproval approval;
  
  public AccusationMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress blamedNode, final DNVPAuthorizationApproval approval) {
    super(tag, sourceId, destID);
    this.blamedNode = blamedNode;
    this.approval = approval;
  }
  
  @Override protected String getContents() {
    return "Blamed node: " + blamedNode.toString() + " " + approval.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE + approval.getSimulatedSize();
  }
}
