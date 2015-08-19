package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;
import interfaces.Sizeable;

import java.util.Random;

import messages.ConnectionRequestApprovedMessage;
import messages.Message;
import messages.SeedNodeMultipleTargetsReplyMessage;
import messages.SeedNodeRequestMessage;
import modules.P2PClient;
import experiment.frameworks.NodeAddress;

public class RandomMinSizeGroupOverlay extends OverlayModule<Object> {
  private final int groupSize;
  private final int timeout = 4;
  private int currentTimeout = timeout;
  
  public RandomMinSizeGroupOverlay(final P2PClient client, final int groupSize, final Random r) {
    super(client, r);
    addIngredient(new RandomGroupBootstrapIngredient(groupSize, new Random(r.nextLong())), client);
    this.groupSize = groupSize;
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      handleSeedNodeMultipleTargetsReplyMessage((SeedNodeMultipleTargetsReplyMessage) message);
    } else if (message instanceof ConnectionRequestApprovedMessage) {
      handleConnectionRequestApprovedMessage((ConnectionRequestApprovedMessage<?>) message);
    }
  }
  
  protected void handleConnectionRequestApprovedMessage(final ConnectionRequestApprovedMessage<?> message) {
    addNeighbor(message.sourceId);
  }
  
  protected void handleSeedNodeMultipleTargetsReplyMessage(final SeedNodeMultipleTargetsReplyMessage message) {
    for (final NodeAddress neighbor : message.targets) {
      if (network.isUp(neighbor)) {
        addNeighbor(neighbor);
        network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), neighbor));
      }
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (currentTimeout > 0) {
      currentTimeout--;
      return;
    }
    if (getNeighbors().size() < groupSize) {
      network.send(new SeedNodeRequestMessage(getMessageTag(), network.getAddress(), network.getServerNode()));
      currentTimeout = timeout;
    }
  }
  
  @Override public void reConnect() {
    // do nothing
  }
}