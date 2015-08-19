package messages.chunkySpread;

import messages.Message;
import experiment.frameworks.NodeAddress;

public abstract class ParentSwapProtocol extends Message {
  final int treeID;
  final boolean isLoadBalancing;
  final NodeAddress thirdParty;
  private static final long serialVersionUID = 1L;
  
  public ParentSwapProtocol(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final int treeID, final NodeAddress thirdParty, final boolean _isLoadBalancing) {
    super(tag, sourceId, destID);
    this.treeID = treeID;
    this.thirdParty = thirdParty;
    isLoadBalancing = _isLoadBalancing;
  }
  
  public NodeAddress getThirdParty() {
    return thirdParty;
  }
  
  public int getTreeID() {
    return treeID;
  }
  
  public boolean isLoadBalancing() {
    return isLoadBalancing;
  }
  
  @Override protected String getContents() {
    return "Tree: " + treeID;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
