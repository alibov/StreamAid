package messages.chunkySpread;

import utils.chunkySpread.LoadState;
import experiment.frameworks.NodeAddress;

public class ParentOverloaded extends LoadStateChange {
  /**
   * 
   */
  private static final long serialVersionUID = 3809427617150872342L;
  private final int treeID;
  
  public ParentOverloaded(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int _treeID,
      final LoadState ls) {
    super(tag, sourceId, destID, ls);
    treeID = _treeID;
  }
  
  public int getTreeID() {
    return treeID;
  }
}
