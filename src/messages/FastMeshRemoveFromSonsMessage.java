package messages;

import experiment.frameworks.NodeAddress;

public class FastMeshRemoveFromSonsMessage extends EmptyMessage {
  public FastMeshRemoveFromSonsMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID) {
    super(tag, sourceId, destID);
  }
  
  private static final long serialVersionUID = 6468481864186L;
}
