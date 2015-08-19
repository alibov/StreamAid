package messages.swapLinks;

import experiment.frameworks.NodeAddress;

public class OnlyOutlinks extends RandomWalk {
  /**
   * 
   */
  private static final long serialVersionUID = 3243995413992360205L;
  
  public OnlyOutlinks(final String tag, final NodeAddress sourceId, final NodeAddress destID, final NodeAddress ori, final int rem) {
    super(tag, sourceId, destID, ori, rem);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
