package messages;

import interfaces.Sizeable;
import experiment.frameworks.NodeAddress;

public class ConnectionRequestDeclinedWithPayload<Payload extends Sizeable> extends ConnectionRequestDeclinedMessage {
  /**
   * 
   */
  private static final long serialVersionUID = 6361252381239167613L;
  public final Payload payload;
  
  public ConnectionRequestDeclinedWithPayload(final String tag, final NodeAddress sourceId, final NodeAddress destID,
      final Payload pay) {
    super(tag, sourceId, destID);
    payload = pay;
  }
  
  @Override protected String getContents() {
    return super.getContents() + payload.toString();
  }
}
