package modules.streaming;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import logging.TextLogger;
import messages.Message;
import messages.ProtocolChangeMessage;
import messages.SeedNodeRequestMessage;
import modules.P2PClient;
import modules.overlays.OverlayModule;

import org.w3c.dom.Node;

import utils.Utils;
import bandits.algo.BanditsAlgorithm;
import experiment.StreamingModuleFactory;
import experiment.frameworks.NodeAddress;

public class AdaptiveStreaming extends StreamingModule {
  private final BanditsAlgorithm banditsAlgo;
  private final int changeTimeout;
  int currTimeout;
  int currentConfNumber = 0;
  int serverConfNumber = 0;
  private final ArrayList<Node> options;
  Set<NodeAddress> knownNodes = new TreeSet<NodeAddress>();
  public final LinkedList<StreamingModule> streamingModules = new LinkedList<StreamingModule>();
  private int serverOption;
  private int serverFromChunk;
  
  @Override public void setServerMode() {
    super.setServerMode();
    currTimeout = changeTimeout;
    // serverConfNumber++;
    client.player.currentConfNumber = serverConfNumber;
    serverOption = banditsAlgo.playNextRound();
    serverFromChunk = 0;
    streamingModules.add(StreamingModuleFactory.initStreamingModule(options.get(serverOption)).getAlg(client));
    for (final StreamingModule sm : streamingModules) {
      sm.setServerMode();
    }
  }
  
  @Override public void handleConsecutiveLag() {
    super.handleConsecutiveLag();
    for (final StreamingModule sm : streamingModules) {
      sm.handleConsecutiveLag();
    }
  }
  
  public AdaptiveStreaming(final P2PClient client, final ArrayList<Node> options, final int changeTimeout,
      final BanditsAlgorithm banditsAlgo, final Random r) {
    super(client, new OverlayModule<Object>(client, r) {
      // empty overlay module
    }, r);
    this.options = options;
    this.changeTimeout = changeTimeout;
    this.banditsAlgo = banditsAlgo;
    if (banditsAlgo.reward != null) {
      addIngredient(banditsAlgo.reward, client);
    }
    if (banditsAlgo.state != null) {
      addIngredient(banditsAlgo.state, client);
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    for (final StreamingModule sm : streamingModules) {
      sm.nextCycle();
    }
    if (streamingModules.isEmpty()) {
      network.send(new SeedNodeRequestMessage(getMessageTag(), network.getAddress(), network.getServerNode()));
    } else if (!streamingModules.getFirst().isActive() /* &&
                                                        * !client.isServerMode() */) {
      TextLogger.log(client.network.getAddress(), streamingModules.getFirst().confNumber + " streaming module removed\n");
      streamingModules.removeFirst().deactivate();
    }
    if (client.isServerMode()) {
      currTimeout--;
      if (currTimeout > 0) {
        return;
      }
      currTimeout = changeTimeout;
      final int newOption = banditsAlgo.playNextRound();
      if (newOption == serverOption) {
        return;
      }
      serverOption = newOption;
      serverConfNumber++;
      serverFromChunk = (int) (Utils.getMovieTime() / 1000 + 10);
      final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
      for (final NodeAddress na : knownNodes) {
        if (!network.isUp(na)) {
          toRemove.add(na);
          continue;
        }
        network.send(new ProtocolChangeMessage(getMessageTag(), network.getAddress(), na, serverConfNumber, options
            .get(serverOption), serverFromChunk));
      }
      knownNodes.removeAll(toRemove);
      handleMessage(new ProtocolChangeMessage(getMessageTag(), network.getAddress(), network.getAddress(), serverConfNumber,
          options.get(serverOption), serverFromChunk));
    }
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ProtocolChangeMessage) {
      if (!streamingModules.isEmpty() && currentConfNumber >= ((ProtocolChangeMessage) message).newConfNumber) {
        TextLogger.log(network.getAddress(), "got conf number " + ((ProtocolChangeMessage) message).newConfNumber
            + " dropping message\n");
        return;
      }
      currentConfNumber = ((ProtocolChangeMessage) message).newConfNumber;
      client.player.newProtocol(currentConfNumber);
      for (final StreamingModule sm : streamingModules) {
        sm.setLatestChunk(((ProtocolChangeMessage) message).fromChunk);
      }
      final StreamingModule module = generateStreamingModule((ProtocolChangeMessage) message);
      module.setConfNumber(currentConfNumber);
      if (!streamingModules.isEmpty()) {
        module.setEarliestChunk(((ProtocolChangeMessage) message).fromChunk + 1);
      }
      if (network.isServerMode()) {
        module.setServerMode();
      }
      streamingModules.add(module);
    } else if (message instanceof SeedNodeRequestMessage) {
      assertServerStatus("non-server received SeedNodeRequestMessage");
      if (!network.isUp(message.sourceId)) {
        return;
      }
      knownNodes.add(message.sourceId);
      network.send(new ProtocolChangeMessage(getMessageTag(), network.getAddress(), message.sourceId, serverConfNumber, options
          .get(serverOption), serverFromChunk));
    }
  }
  
  private StreamingModule generateStreamingModule(final ProtocolChangeMessage message) {
    return StreamingModuleFactory.initStreamingModule(message.node).getAlg(client);
  }
}
