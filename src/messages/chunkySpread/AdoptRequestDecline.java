package messages.chunkySpread;

import experiment.frameworks.NodeAddress;

public class AdoptRequestDecline extends ParentSwapProtocol {
  /**
   * 
   */
  private static final long serialVersionUID = 208824631245094746L;
  
  public AdoptRequestDecline(final String tag, final NodeAddress sourceId, final NodeAddress destID, final int treeID,
      final NodeAddress thirdParty, final boolean _isLoadBalancing) {
    super(tag, sourceId, destID, treeID, thirdParty, _isLoadBalancing);
  }
}
