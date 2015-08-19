package messages.chunkySpread;

import experiment.frameworks.NodeAddress;

public class RenounceParent extends ParentSwapProtocol {
  /**
   * 
   */
  private static final long serialVersionUID = -3875430754470496974L;
  
  public RenounceParent(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int treeID,
      final NodeAddress thirdParty, final boolean _isLoadBalancing) {
    super(tag, sourceId, destID, treeID, thirdParty, _isLoadBalancing);
  }
}