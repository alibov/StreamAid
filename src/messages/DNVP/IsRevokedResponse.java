package messages.DNVP;

import messages.Message;
import entites.DNVP.DNVPAuthorizationApproval;
import experiment.frameworks.NodeAddress;

public class IsRevokedResponse extends Message {
  private static final long serialVersionUID = 1660750167807260198L;
  public DNVPAuthorizationApproval approval;
  
  public IsRevokedResponse(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final DNVPAuthorizationApproval approval) {
    super(tag, sourceId, destID);
    this.approval = approval;
  }
  
  @Override protected String getContents() {
    return approval.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + approval.getSimulatedSize();
  }
}
