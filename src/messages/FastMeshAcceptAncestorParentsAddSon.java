package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshAcceptAncestorParentsAddSon extends Message {
  public final NodeAddress ancestorToReplace;
  
  public FastMeshAcceptAncestorParentsAddSon(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final NodeAddress ancestorToReplace) {
    super(tag, sourceId, destID);
    this.ancestorToReplace = ancestorToReplace;
  }
  
  /**
   * 
   */
  private static final long serialVersionUID = 6844186187611L;
  
  @Override protected String getContents() {
    // TODO Auto-generated method stub
    return "";
  }
  
  @Override public boolean isOverheadMessage() {
    // TODO Auto-generated method stub
    return true;
  }
}
