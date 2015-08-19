package ingredients.bootstrap;

import ingredients.AbstractIngredient;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.Message;
import messages.SeedNodeRequestMessage;
import messages.SeedNodeSingleTargetReplyMessage;
import modules.P2PClient;
import modules.overlays.OverlayModule;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class RandomSeedBootstrapIngredient extends AbstractIngredient<OverlayModule<?>> {
  protected Set<NodeAddress> knownNodes;
  private final int timeout = 5;
  private int currentTimeout = 1;
  boolean enableBackoff = false;
  Class<? extends Message> addToKnownMessageClass;
  Map<NodeAddress, Integer> backoff = new TreeMap<NodeAddress, Integer>();
  Map<NodeAddress, Integer> backoffTimes = new TreeMap<NodeAddress, Integer>();
  
  public RandomSeedBootstrapIngredient(final Random r) {
    this(SeedNodeRequestMessage.class, r);
  }
  
  public RandomSeedBootstrapIngredient(final Class<? extends Message> addToKnownMessageClass, final Random r) {
    super(r);
    this.addToKnownMessageClass = addToKnownMessageClass;
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final OverlayModule<?> alg) {
    super.setClientAndComponent(client, alg);
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    knownNodes = new TreeSet<NodeAddress>();
    knownNodes.add(client.network.getAddress());
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof SeedNodeRequestMessage) {
      assertServerStatus("SeedNodeRequestMessage requested from " + client.network.getAddress());
      if (!backoff.containsKey(message.sourceId) && client.network.isUp(message.sourceId)) {
        client.network.send(new SeedNodeSingleTargetReplyMessage(alg.getMessageTag(), client.network.getAddress(),
            message.sourceId, getTargetNode(message.sourceId)));
        Utils.checkExistence(backoffTimes, message.sourceId, 0);
        if (enableBackoff) {
          backoff.put(message.sourceId, (int) Math.pow(2, backoffTimes.get(message.sourceId)));
        }
      }
    } else if (message instanceof SeedNodeSingleTargetReplyMessage) {
      if (Utils.movieStartTime == -1) {
        Utils.movieStartTime = Utils.getTime() - ((SeedNodeSingleTargetReplyMessage) message).movieStartOffset;
        TextLogger.log(client.network.getAddress(), "initializing movie start time to " + Utils.movieStartTime + "\n");
      }
    }
    if (message.getClass().equals(addToKnownMessageClass)) {
      knownNodes.add(message.sourceId);
    }
  }
  
  private NodeAddress getTargetNode(final NodeAddress node) {
    NodeAddress target = null;
    final ArrayList<NodeAddress> seedNodes = new ArrayList<NodeAddress>(getSeedNodesGroup());
    seedNodes.remove(node);
    do {
      if (target != null) {
        knownNodes.remove(target);
        seedNodes.remove(target);
      }
      target = seedNodes.get(r.nextInt(seedNodes.size()));
    } while (!client.network.isUp(target));
    return target;
  }
  
  protected Set<NodeAddress> getSeedNodesGroup() {
    return knownNodes;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (client.isServerMode()) {
      final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
      for (final NodeAddress entry : backoff.keySet()) {
        if (backoff.get(entry).equals(1)) {
          client.network.send(new SeedNodeSingleTargetReplyMessage(alg.getMessageTag(), client.network.getAddress(), entry,
              getTargetNode(entry)));
          backoffTimes.put(entry, backoffTimes.get(entry) + 1);
          toRemove.add(entry);
        } else {
          backoff.put(entry, backoff.get(entry) - 1);
        }
      }
      backoff.keySet().removeAll(toRemove);
      return;
    }
    // timer for connections init
    if (currentTimeout > 0) {
      currentTimeout--;
    }
    // if disconnected from the network and not waiting for seed target
    if (!alg.isOverlayConnected()) {
      initConnections();
    }
  }
  
  public void initConnections() {
    if (currentTimeout <= 0 && !client.isServerMode()) {
      client.network.send(new SeedNodeRequestMessage(getMessageTag(), client.network.getAddress(), client.network.getServerNode()));
      currentTimeout = timeout;
    }
  }
}
