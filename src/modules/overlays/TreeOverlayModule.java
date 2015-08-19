package modules.overlays;

import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import modules.P2PClient;
import experiment.frameworks.NodeAddress;

public abstract class TreeOverlayModule<T> extends OverlayModule<T> {
  protected NodeAddress fatherNode = null;
  
  public TreeOverlayModule(final P2PClient client, final Random r) {
    super(client, r);
  }
  
  public NodeAddress getFatherNode() {
    return fatherNode;
  }
  
  public Set<NodeAddress> getSonNodes() {
    final TreeSet<NodeAddress> retVal = new TreeSet<NodeAddress>(getNeighbors());
    if (fatherNode != null) {
      retVal.remove(fatherNode);
    }
    return retVal;
  }
}
