package ingredients.overlay;

import ingredients.AbstractIngredient;

import java.util.Set;

import messages.DisconnectMessage;
import messages.Message;
import modules.overlays.OverlayModule;
import experiment.frameworks.NodeAddress;

public class DoubleRemoveIngredient extends AbstractIngredient<OverlayModule<?>> {
  public DoubleRemoveIngredient() {
    super(null);
  }
  
  // TODO why this and the removeNeighbors return values are different?
  public void removeNeighbor(final NodeAddress neighbor) {
    alg.removeNeighbor(neighbor);
    client.network.send(new DisconnectMessage(getMessageTag(), client.network.getAddress(), neighbor));
  }
  
  public boolean removeNeighbors(final Set<NodeAddress> toRemove) {
    final boolean retVal = alg.removeNeighbors(toRemove);
    for (final NodeAddress neighbor : toRemove) {
      client.network.send(new DisconnectMessage(getMessageTag(), client.network.getAddress(), neighbor));
    }
    return retVal;
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof DisconnectMessage) {
      if (alg.removeNeighbor(message.sourceId) && client.network.isUp(message.sourceId)) {
        client.network.send(new DisconnectMessage(getMessageTag(), client.network.getAddress(), message.sourceId));
      }
    }
  }
}
