package messages;

import experiment.frameworks.NodeAddress;

public class DeputyRequestMessage extends EmptyMessage {
  public DeputyRequestMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  private static final long serialVersionUID = 4304197536858405826L;
}
