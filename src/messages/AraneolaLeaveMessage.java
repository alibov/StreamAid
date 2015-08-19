package messages;

import experiment.frameworks.NodeAddress;

public class AraneolaLeaveMessage extends EmptyMessage {
  private static final long serialVersionUID = 5365451869316474955L;
  
  public AraneolaLeaveMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
