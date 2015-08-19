package modules.network.ip;

import messages.Message;
import experiment.frameworks.NodeAddress;

public class PongMessage extends Message {
  private static final long serialVersionUID = 6142107944032711526L;
  
  public PongMessage(final NodeAddress sourceId) {
    super("pong", sourceId, null);
  }
  
  @Override protected String getContents() {
    return null;
  }
  
  @Override public boolean isOverheadMessage() {
    return true;
  }
}
