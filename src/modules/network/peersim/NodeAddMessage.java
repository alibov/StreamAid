package modules.network.peersim;

import messages.EmptyMessage;

public class NodeAddMessage extends EmptyMessage {
  private static final long serialVersionUID = -5006220481701057305L;
  public int amount;
  
  public NodeAddMessage(final int amount) {
    super("NodeAddMessage", null, null);
    this.amount = amount;
  }
}
