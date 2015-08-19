package messages.chunkySpread;

import experiment.frameworks.NodeAddress;

public class BeginParentSwap extends ParentSwapProtocol {
  /**
   * 
   */
  private static final long serialVersionUID = 2298543033149928840L;
  
  public BeginParentSwap(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int treeID,
      final NodeAddress thirdParty, final boolean _isLoadBalancing) {
    super(tag, sourceId, destID, treeID, thirdParty, _isLoadBalancing);
  }
}
