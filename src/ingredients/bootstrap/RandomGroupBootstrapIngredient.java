package ingredients.bootstrap;

import ingredients.AbstractIngredient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import logging.TextLogger;
import messages.Message;
import messages.SeedNodeMultipleTargetsReplyMessage;
import messages.SeedNodeRequestMessage;
import modules.P2PClient;
import modules.overlays.OverlayModule;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class RandomGroupBootstrapIngredient extends AbstractIngredient<OverlayModule<?>> {
  protected Set<NodeAddress> knownNodes;
  private final int timeout = 5;
  private int currentTimeout = 1;
  public final int groupSize;
  
  public RandomGroupBootstrapIngredient(final int groupSize, final Random r) {
    super(r);
    this.groupSize = groupSize;
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final OverlayModule<?> alg) {
    super.setClientAndComponent(client, alg);
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof SeedNodeRequestMessage) {
      if (!client.network.isUp(message.sourceId)) {
        return;
      }
      assertServerStatus("SeedNodeRequestMessage requested from " + client.network);
      final ArrayList<NodeAddress> seedNodes = new ArrayList<NodeAddress>(knownNodes);
      seedNodes.remove(message.sourceId);
      NodeAddress target = null;
      final List<NodeAddress> chosenNodes = new LinkedList<NodeAddress>();
      Collections.shuffle(seedNodes, r);
      while (chosenNodes.size() < groupSize && !seedNodes.isEmpty()) {
        target = seedNodes.get(seedNodes.size() - 1);
        if (!client.network.isUp(target)) {
          knownNodes.remove(target);
        } else {
          chosenNodes.add(target);
        }
        seedNodes.remove(target);
      }
      if (chosenNodes.contains(client.network.getAddress())) {
        chosenNodes.remove(client.network.getAddress());
        chosenNodes.add(message.destID);
      }
      client.network.send(new SeedNodeMultipleTargetsReplyMessage(alg.getMessageTag(), client.network.getAddress(),
          message.sourceId, chosenNodes));
      knownNodes.add(message.sourceId);
    } else if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      if (Utils.movieStartTime == -1) {
        Utils.movieStartTime = Utils.getTime() - ((SeedNodeMultipleTargetsReplyMessage) message).movieStartOffset;
        TextLogger.log(client.network.getAddress(), "initializing movie start time to " + Utils.movieStartTime + "\n");
      }
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    // timer for connections init
    if (currentTimeout > 0) {
      currentTimeout--;
    }
    // if disconnected from the network and not waiting for seed target
    if (!alg.isOverlayConnected()) {
      if (currentTimeout <= 0) {
        initConnections();
        currentTimeout = timeout;
      }
    }
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    knownNodes = new TreeSet<NodeAddress>();
    knownNodes.add(client.network.getAddress());
  }
  
  protected void initConnections() {
    if (!client.isServerMode()) {
      client.network.send(new SeedNodeRequestMessage(alg.getMessageTag(), client.network.getAddress(), client.network
          .getServerNode()));
    }
  }
}
