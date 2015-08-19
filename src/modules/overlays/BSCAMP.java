package modules.overlays;

import interfaces.Sizeable;

import java.util.Collection;
import java.util.Random;

import messages.ConnectionRequestApprovedMessage;
import messages.ConnectionRequestDeclinedMessage;
import messages.Message;
import modules.P2PClient;
import experiment.frameworks.NodeAddress;

/**
 * Implementation if a bidirectional SCAMP overlay
 *
 * @author Alex Libov
 *
 */
public class BSCAMP extends SCAMP {
  public BSCAMP(final P2PClient client, final int c, final Random r) {
    super(client, c, r);
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ConnectionRequestApprovedMessage) {
      super.addNeighbor(message.sourceId);
    }
    if (message instanceof ConnectionRequestDeclinedMessage) {
      if (super.removeNeighbor(message.sourceId)) {
        network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), network.getAddress(), message.sourceId));
      }
    }
  }
  
  @Override public void addNeighbor(final NodeAddress neighbor) {
    super.addNeighbor(neighbor);
    network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), neighbor));
  }
  
  @Override public boolean removeNeighbor(final NodeAddress toRemove) {
    final boolean retVal = super.removeNeighbor(toRemove);
    if (retVal) {
      network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), network.getAddress(), toRemove));
    }
    return retVal;
  }
  
  @Override public boolean removeNeighbors(final Collection<NodeAddress> toRemove) {
    final boolean retVal = super.removeNeighbors(toRemove);
    for (final NodeAddress neighbor : toRemove) {
      network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), network.getAddress(), neighbor));
    }
    return retVal;
  }
}
