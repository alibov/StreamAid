package messages.chunkySpread;

import utils.chunkySpread.LoadState;
import experiment.frameworks.NodeAddress;

public class NeighborLoadChange extends LoadStateChange {
  /**
   * 
   */
  private static final long serialVersionUID = 4056563730899253675L;
  
  public NeighborLoadChange(final String tag, final NodeAddress sourceId, final NodeAddress destID, final LoadState ls) {
    super(tag, sourceId, destID, ls);
  }
}
