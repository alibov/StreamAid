package messages;

import experiment.frameworks.NodeAddress;
import interfaces.Sizeable;

public class PrimeRequestMessage<Payload extends Sizeable> extends Message {
  public final Payload payload;
  
  @Override public long getSimulatedSize() {
    return super.getSimulatedSize() + (payload == null ? 0 : payload.getSimulatedSize());
  }
  
  private static final long serialVersionUID = -8759555870619264726L;
  
  public PrimeRequestMessage(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final Payload payload) {
    super(tag, sourceId, destID);
    this.payload = payload;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
  
  @Override protected String getContents() {
    return payload == null ? "" : payload.toString();
  }
}
