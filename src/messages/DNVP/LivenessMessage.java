package messages.DNVP;

import java.util.Map;

import messages.Message;
import entites.DNVP.Nonce;
import experiment.frameworks.NodeAddress;

public class LivenessMessage extends Message {
  private static final long serialVersionUID = -3867303433721992132L;
  public Map<NodeAddress, Nonce> nonces;
  
  public LivenessMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final Map<NodeAddress, Nonce> nonces) {
    super(tag, sourceId, destID);
    this.nonces = nonces;
  }
  
  @Override protected String getContents() {
    return nonces.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + nonces.size() * (NodeAddress.SIZE + Nonce.SIZE);
  }
}
