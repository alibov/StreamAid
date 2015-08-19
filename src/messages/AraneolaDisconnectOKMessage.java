package messages;

import experiment.frameworks.NodeAddress;

public class AraneolaDisconnectOKMessage extends EmptyMessage {
  private static final long serialVersionUID = 5694078948006305213L;
  
  public AraneolaDisconnectOKMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
