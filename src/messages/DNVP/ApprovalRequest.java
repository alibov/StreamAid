package messages.DNVP;

import java.util.Set;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class ApprovalRequest extends Message {
  private static final long serialVersionUID = -3414650766670975468L;
  public Set<NodeAddress> neighborsToApprove;
  
  public ApprovalRequest(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final Set<NodeAddress> neighborsToApprove) {
    super(tag, sourceId, destID);
    this.neighborsToApprove = neighborsToApprove;
  }
  
  @Override protected String getContents() {
    return neighborsToApprove.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE * neighborsToApprove.size();
  }
}
