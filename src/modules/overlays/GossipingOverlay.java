package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;
import interfaces.Sizeable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import messages.ConnectionRequestApprovedMessage;
import messages.Message;
import messages.PartialMembershipViewMessage;
import messages.SeedNodeMultipleTargetsReplyMessage;
import modules.P2PClient;
import experiment.frameworks.NodeAddress;

public class GossipingOverlay extends OverlayModule<Object> {
  private final int gossipDelay;
  private int currentDelay;
  private final int amountToSend;
  private final int groupSize;
  
  public GossipingOverlay(final P2PClient client, final int groupSize, final int gossipdelay, final int amounttosend, final Random r) {
    super(client, r);
    addIngredient(new RandomGroupBootstrapIngredient(groupSize, new Random(r.nextLong())), client);
    gossipDelay = gossipdelay;
    currentDelay = gossipDelay;
    amountToSend = amounttosend;
    this.groupSize = groupSize;
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof PartialMembershipViewMessage) {
      final Set<NodeAddress> neighborsList = ((PartialMembershipViewMessage<?>) message).infoMap.keySet();
      // add the neighbors received in the message
      for (final NodeAddress newNeighbor : neighborsList) {
        addNeighbor(newNeighbor);
      }
      addNeighbor(message.sourceId);
      // remove random neighbors until reach groupSize
      while (getNeighbors().size() > groupSize) {
        final List<NodeAddress> nList = new ArrayList<NodeAddress>(getNeighbors());
        removeNeighbor(nList.get(r.nextInt(nList.size())));
      }
    } else if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
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
    if (currentDelay > 0) {
      currentDelay--;
    }
    if (currentDelay == 0) {
      currentDelay = gossipDelay;
      sendGossipMessages(this, getNeighbors(), getNeighbors(), amountToSend, r);
    }
  }
  
  public static void sendGossipMessages(final GossipingOverlay alg, final Set<NodeAddress> neighbors,
      final Set<NodeAddress> gossipInfo, final int amountToSend, final Random r) {
    final Map<NodeAddress, Sizeable> map = new TreeMap<NodeAddress, Sizeable>();
    for (final NodeAddress node : gossipInfo) {
      map.put(node, null);
    }
    sendGossipMessages(alg, neighbors, map, amountToSend, r);
  }
  
  public static <T extends Sizeable> void sendGossipMessages(final OverlayModule<?> alg, final Set<NodeAddress> neighbors,
      final Map<NodeAddress, T> gossipInfo, final int amountToSend, final Random r) {
    if (neighbors.size() > 2) {
      final List<NodeAddress> sendList = new ArrayList<NodeAddress>(gossipInfo.keySet());
      for (final NodeAddress neighbor : neighbors) {
        final boolean removed = sendList.remove(neighbor);
        if (sendList.size() > amountToSend) {
          Collections.shuffle(sendList, r);
        }
        final Map<NodeAddress, T> map = new TreeMap<NodeAddress, T>();
        final List<NodeAddress> sublist = sendList.subList(0, Math.min(amountToSend, sendList.size()));
        for (final NodeAddress node : sublist) {
          map.put(node, gossipInfo.get(node));
        }
        alg.network.send(new PartialMembershipViewMessage<T>(alg.getMessageTag(), alg.network.getAddress(), neighbor, map));
        if (removed) {
          sendList.add(neighbor);
        }
      }
    }
  }
}