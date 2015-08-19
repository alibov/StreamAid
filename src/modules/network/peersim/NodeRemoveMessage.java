package modules.network.peersim;

import messages.EmptyMessage;

public class NodeRemoveMessage extends EmptyMessage {
  private static final long serialVersionUID = -1910444137255807565L;
  public int amount;
  
  public NodeRemoveMessage(final int amount) {
    super("NodeRemoveMessage", null, null);
    this.amount = amount;
  }
}
