package messages;

import experiment.frameworks.NodeAddress;

public class AraneolaDisconnectMessage extends EmptyMessage {
  private static final long serialVersionUID = 8046704369341161957L;
  
  public AraneolaDisconnectMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
