package modules.network.peersim;

import messages.EmptyMessage;
import experiment.frameworks.NodeAddress;

public class SessionEndMessage extends EmptyMessage {
  private static final long serialVersionUID = -4841761112605623330L;
  
  public SessionEndMessage(final NodeAddress id) {
    super("SessionEndMessage", id, id);
  }
}
