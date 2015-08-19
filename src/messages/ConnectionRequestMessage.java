package messages;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;

public class ConnectionRequestMessage<Payload extends Sizeable> extends Message {
  private static final long serialVersionUID = -3789942349576357488L;
  public final Payload payload;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + (payload == null ? 0 : payload.getSimulatedSize());
  }
  
  public ConnectionRequestMessage(final String tag, final NodeAddress sourceId,
      final NodeAddress destID, final Payload payload) {
    super(tag, sourceId, destID);
    if (sourceId.equals(destID)) {
      throw new RuntimeException("can't connect to yourself!");
    }
    this.payload = payload;
  }
  
  public ConnectionRequestMessage(final String tag, final NodeAddress sourceId,
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
