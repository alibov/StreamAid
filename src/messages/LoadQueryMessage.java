package messages;

import experiment.frameworks.NodeAddress;

public class LoadQueryMessage extends EmptyMessage {
  private static final long serialVersionUID = -2244306793250156702L;
  
  public LoadQueryMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
