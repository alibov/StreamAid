package messages.DNVP;

import java.util.LinkedList;

import messages.Message;
import entites.DNVP.Nonce;
import experiment.frameworks.NodeAddress;

public class NewNoncesMessage extends Message {
  private static final long serialVersionUID = -5827642213021189532L;
  public NodeAddress deliverToNeighbor;
  public LinkedList<Nonce> nonces;
  
  public NewNoncesMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final LinkedList<Nonce> nonces, final NodeAddress deliverToNeighbor) {
    super(tag, sourceId, destID);
    this.nonces = nonces;
    this.deliverToNeighbor = deliverToNeighbor;
  }
  
  @Override protected String getContents() {
    return "Deliver to neighbor: " + deliverToNeighbor.toString() + " Nonces: " + nonces.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE + Nonce.SIZE * nonces.size();
  }
}
