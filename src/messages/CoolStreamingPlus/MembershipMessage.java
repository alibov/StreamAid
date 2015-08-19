package messages.CoolStreamingPlus;

import java.util.HashSet;
import java.util.Set;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class MembershipMessage extends Message {
  public Set<NodeAddress> nodes;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE * nodes.size();
  }
  
  public MembershipMessage(final Set<NodeAddress> nodes, final String tag,
      final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
    this.nodes = new HashSet<NodeAddress>(nodes);
  }
  
  private static final long serialVersionUID = -7555691347452453471L;
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "";
  }
}
