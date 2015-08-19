package messages;

import experiment.frameworks.NodeAddress;

public class StateRequest extends EmptyMessage {
  private static final long serialVersionUID = -5970431460237110320L;
  
  public StateRequest(final String tag, final NodeAddress sourceId, final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
}
