package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;
import interfaces.Sizeable;

import java.util.Random;

import messages.ConnectionRequestApprovedMessage;
import messages.ConnectionRequestDeclinedMessage;
import messages.ConnectionRequestMessage;
import messages.Message;
import messages.SeedNodeMultipleTargetsReplyMessage;
import modules.P2PClient;
import utils.Common;
import experiment.frameworks.NodeAddress;

public class PrimeOverlay extends GroupedOverlayModule<Object> {
  private static double BWPF;
  
  public PrimeOverlay(final P2PClient client, final int groupSize, final Random r) {
    super(client, r);
    addIngredient(new RandomGroupBootstrapIngredient(groupSize, new Random(r.nextLong())), client);
    BWPF = ((double) Common.currentConfiguration.bitRate) / (Common.currentConfiguration.descriptions);
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof ConnectionRequestDeclinedMessage) {
      // Do nothing
    } else if (message instanceof ConnectionRequestMessage) {
      handleConnectionRequestMessage((ConnectionRequestMessage<?>) message);
    } else if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      handleSeedNodeMultipleTargetsReplyMessage((SeedNodeMultipleTargetsReplyMessage) message);
    } else if (message instanceof ConnectionRequestApprovedMessage) {
      handleConnectionRequestApprovedMessage((ConnectionRequestApprovedMessage<?>) message);
    }
  }
  
  protected void handleConnectionRequestApprovedMessage(final ConnectionRequestApprovedMessage<?> message) {
    addToGroup(GroupedOverlayModule.fathersGroupName, message.sourceId);
  }
  
  protected void handleConnectionRequestMessage(final ConnectionRequestMessage<?> message) {
    if ((((double) network.getUploadBandwidth()) / (getNodeGroup(GroupedOverlayModule.sonsGroupName).size() + 1)) >= BWPF) {
      addToGroup(GroupedOverlayModule.sonsGroupName, message.sourceId);
      network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), message.sourceId));
    } else {
      network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), network.getAddress(), message.sourceId));
    }
  }
  
  /* We can't use super method because in our case not all connections are
   * approved and we need to ask the node for approval before we can add him as
   * our father. */
  protected void handleSeedNodeMultipleTargetsReplyMessage(final SeedNodeMultipleTargetsReplyMessage message) {
    for (final NodeAddress father : message.targets) {
      if (network.isUp(father)) {
        network.send(new ConnectionRequestMessage<Sizeable>(getMessageTag(), network.getAddress(), father));
      }
    }
  }
}
