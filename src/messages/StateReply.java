package messages;

import experiment.frameworks.NodeAddress;

public class StateReply extends Message {
  private static final long serialVersionUID = 7357466197674277958L;
  final public String state;
  
  public StateReply(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final String state) {
    super(tag, sourceId, destID);
    this.state = state;
  }
  
  @Override protected String getContents() {
    return state;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
