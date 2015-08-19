package messages;

import experiment.frameworks.NodeAddress;

public class AliveMessage extends EmptyMessage {
  private static final long serialVersionUID = -393393942017628332L;
  
  public AliveMessage(String tag, NodeAddress sourceId, NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
