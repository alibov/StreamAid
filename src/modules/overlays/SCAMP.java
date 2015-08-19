package modules.overlays;

import ingredients.bootstrap.RandomSeedBootstrapIngredient;
import interfaces.Sizeable;

import java.util.Random;
import java.util.Set;

import logging.TextLogger;
import messages.ConnectionRequestForwardMessage;
import messages.ConnectionRequestMessage;
import messages.Message;
import messages.SeedNodeSingleTargetReplyMessage;
import modules.P2PClient;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class SCAMP extends OverlayModule<Object> {
  private final int c;
  
  public SCAMP(final P2PClient client, final int c, final Random r) {
    super(client, r);
    addIngredient(new RandomSeedBootstrapIngredient(new Random(r.nextLong())), client);
    this.c = c;
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof SeedNodeSingleTargetReplyMessage) {
      if (!network.isUp(((SeedNodeSingleTargetReplyMessage) message).target)) {
        return;
      }
      addNeighbor(((SeedNodeSingleTargetReplyMessage) message).target);
      network.send(new ConnectionRequestMessage<Sizeable>(getMessageTag(), network.getAddress(),
          ((SeedNodeSingleTargetReplyMessage) message).target));
    } else if (message instanceof ConnectionRequestMessage) {
      if (getNeighbors().contains(message.sourceId)) {
        TextLogger.log(network.getAddress(), "already a neighbor of " + message.sourceId + " returning\n");
        return;
      }
      final Set<NodeAddress> neighbors = getNeighbors();
      for (final NodeAddress neighbor : neighbors) {
        network.send(new ConnectionRequestForwardMessage(getMessageTag(), network.getAddress(), neighbor, message.sourceId));
      }
      final NodeAddress[] array = neighbors.toArray(new NodeAddress[0]);
      for (int i = 0; i < c && array.length > 0; i++) {
        final NodeAddress target = array[r.nextInt(array.length)];
        network.send(new ConnectionRequestForwardMessage(getMessageTag(), network.getAddress(), target, message.sourceId));
      }
      addNeighbor(message.sourceId);
      // node.send(new ConnectionRequestApprovedMessage<Object>(getMessageTag(),
      // node.getImpl(), message.sourceId));
    } else if (message instanceof ConnectionRequestForwardMessage) {
      final NodeAddress target = ((ConnectionRequestForwardMessage) message).newNode;
      double keep = r.nextDouble();
      keep = Math.floor((getNeighbors().size() + 1) * keep);
      if (keep == 0 && !getNeighbors().contains(target) && !network.getAddress().equals(target)) {
        addNeighbor(target);
        // node.send(new
        // ConnectionRequestApprovedMessage<Object>(getMessageTag(),
        // node.getImpl(), target));
      } else {
        if (getNeighbors().size() == 0 || (getNeighbors().size() == 1 && getNeighbors().contains(target))) {
          return;
        }
        final NodeAddress nextNode = Utils.pickRandomElementExcept(getNeighbors(), target, r);
        network.send(new ConnectionRequestForwardMessage(getMessageTag(), network.getAddress(), nextNode, target));
      }
    }
  }
}
