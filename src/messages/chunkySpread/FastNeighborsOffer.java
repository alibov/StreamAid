package messages.chunkySpread;

import java.util.Set;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class FastNeighborsOffer extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = -7491202416270325970L;
  private final int treeID;
  private final Set<NodeAddress> fastNeighbors;
  
  public FastNeighborsOffer(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int treeID,
      final Set<NodeAddress> _fastNeighbors) {
    super(tag, sourceId, destID);
    this.treeID = treeID;
    fastNeighbors = _fastNeighbors;
  }
  
  public Set<NodeAddress> getFastNeighbors() {
    return fastNeighbors;
  }
  
  public int getTreeID() {
    return treeID;
  }
  
  @Override protected String getContents() {
    return null;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
