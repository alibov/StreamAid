package messages;

import experiment.frameworks.NodeAddress;

public class DisconnectMessage extends EmptyMessage {
  public DisconnectMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  private static final long serialVersionUID = -7417216418730535508L;
}
