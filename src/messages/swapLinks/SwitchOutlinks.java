package messages.swapLinks;

import messages.Message;
import experiment.frameworks.NodeAddress;

/**
 * remove the source from my inlinks, then make an outlink to the messages'
 * origin
 * 
 */
public class SwitchOutlinks extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = 8260878873270658839L;
  final public NodeAddress target;
  
  public SwitchOutlinks(final String tag, final NodeAddress sourceId, final NodeAddress destID, final NodeAddress tar) {
    super(tag, sourceId, destID);
    target = tar;
  }
  
  @Override protected String getContents() {
    return "switch outlink from " + sourceId + " , to " + target;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
