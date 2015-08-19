package messages;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;

public class ConnectionRequestApprovedMessage<Payload extends Sizeable> extends Message {
  private static final long serialVersionUID = 3762143296221904686L;
  public final Payload payload;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + (payload == null ? 0 : payload.getSimulatedSize());
  }
  
  public ConnectionRequestApprovedMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Payload payload) {
    super(tag, sourceId, destID);
    this.payload = payload;
  }
  
  public ConnectionRequestApprovedMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID) {
    this(tag, sourceId, destID, null);
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return payload == null ? "" : payload.toString();
  }
}
