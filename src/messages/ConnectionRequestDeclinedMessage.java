package messages;

import experiment.frameworks.NodeAddress;

public class ConnectionRequestDeclinedMessage extends EmptyMessage {
  private static final long serialVersionUID = -3602554062072639205L;
  
  public ConnectionRequestDeclinedMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
