package messages.chunkySpread;

import java.util.List;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class UnderLoadedNeighborsOffer extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 1001129436566301457L;
  private final int treeID;
  private final List<NodeAddress> offeredParents;
  
  public UnderLoadedNeighborsOffer(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int treeID,
      final List<NodeAddress> _offeredParents) {
    super(tag, sourceId, destID);
    this.treeID = treeID;
    offeredParents = _offeredParents;
  }
  
  public List<NodeAddress> getOfferedParents() {
    return offeredParents;
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
