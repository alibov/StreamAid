package messages;

import java.util.TreeSet;

import experiment.frameworks.NodeAddress;

public class KnownNodesInClubResponse extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = -1542278643681979982L;
  public final TreeSet<NodeAddress> cands;
  
  public KnownNodesInClubResponse(final String messageTag, final NodeAddress impl, final NodeAddress sourceId,
      final TreeSet<NodeAddress> cands) {
    super(messageTag, impl, sourceId);
    this.cands = cands;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + cands.size() * NodeAddress.SIZE;
  }
  
  @Override protected String getContents() {
    return cands.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
