package messages.DNVP;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class NoncesDelivered extends Message {
  private static final long serialVersionUID = 3285655778715369569L;
  public NodeAddress neighborWhoGotNonces;
  public Long roundNoncesIssued;
  
  public NoncesDelivered(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final NodeAddress neighborWhoGotNonces, final Long roundNoncesIssued) {
    super(tag, sourceId, destID);
    this.neighborWhoGotNonces = neighborWhoGotNonces;
    this.roundNoncesIssued = roundNoncesIssued;
  }
  
  @Override protected String getContents() {
    return "Node: " + neighborWhoGotNonces.toString() + " , Round: " + roundNoncesIssued.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE + Long.SIZE;
  }
}
