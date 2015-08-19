package messages.DNVP;

import java.util.Set;
import java.util.TreeSet;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class RemoveNeighborsNonces extends Message {
  private static final long serialVersionUID = 8471704600656815407L;
  public Set<NodeAddress> nodesToRemove = new TreeSet<NodeAddress>();
  
  public RemoveNeighborsNonces(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Set<NodeAddress> nodesToRemove) {
    super(tag, sourceId, destID);
    this.nodesToRemove.addAll(nodesToRemove);
  }
  
  @Override protected String getContents() {
    return nodesToRemove.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + NodeAddress.SIZE * nodesToRemove.size();
  }
}
