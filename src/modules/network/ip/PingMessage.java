package modules.network.ip;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class PingMessage extends Message {
  private static final long serialVersionUID = -270714642191255408L;
  
  public PingMessage(final NodeAddress sourceId, final NodeAddress destId) {
    super("ping", sourceId, destId);
  }
  
  @Override protected String getContents() {
    return "";
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
