package messages;

import experiment.frameworks.NodeAddress;

public abstract class EmptyMessage extends Message {
  private static final long serialVersionUID = -5795103650119011385L;
  
  public EmptyMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return "";
  }
}
