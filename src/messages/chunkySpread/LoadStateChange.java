package messages.chunkySpread;

import messages.Message;
import utils.chunkySpread.LoadState;
import experiment.frameworks.NodeAddress;

public abstract class LoadStateChange extends Message {
  /**
   * 
   */
  private static final long serialVersionUID = -2677984547524972359L;
  private final LoadState toState;
  
  public LoadStateChange(final String tag, final NodeAddress sourceId, final NodeAddress destID, final LoadState ls) {
    super(tag, sourceId, destID);
    toState = ls;
  }
  
  public LoadState getToState() {
    return toState;
  }
  
  @Override protected String getContents() {
    return toState.toString();
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}