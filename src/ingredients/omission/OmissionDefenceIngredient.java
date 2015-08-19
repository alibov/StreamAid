package ingredients.omission;

import ingredients.AbstractIngredient;
import ingredients.DNVP.DNVPOverlayIngredient;
import ingredients.network.LogChunkMessages;
import ingredients.overlay.security.DNVPAuditingIngredient;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.Message;
import messages.OmissionDefense.BlameMessage;
import messages.OmissionDefense.DefendMessage;
import messages.OmissionDefense.OmissionRequestMessage;
import messages.OmissionDefense.OmissionResponseMessage;
import modules.streaming.StreamingModule;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class OmissionDefenceIngredient extends AbstractIngredient<StreamingModule> {
  private final Map<Long, Set<Long>> cycleToMissingChunks = new TreeMap<Long, Set<Long>>();
  private final Map<Long, Set<NodeAddress>> cycleToNeighbors = new TreeMap<Long, Set<NodeAddress>>();
  // A mapping between a neighbor and its neighbor neighbor who received a
  // request from this node. In case there is no answer from the neighbor
  // neighbor for several rounds we need to remove matching neighbor.
  private final Map<NodeAddress /* my neighbor */, NodeAddress/* the request
                                                               * receiver */> sentRequests = new TreeMap<NodeAddress, NodeAddress>();
  private final Map<NodeAddress /* my neighbor */, Long /* the cycle the request
                                                         * sent */> sentRequestsCycle = new TreeMap<NodeAddress, Long>();
  private final int numOfCycles;
  private final int thresholdForMisses;
  private final int roundsForDiffusion;
  private final int roundsForAnswer;
  LogChunkMessages lcm;
  
  public OmissionDefenceIngredient(final int numOfCycles, final int thresholdForMisses, final int roundsForDiffusion,
      final int roundsForAnswer, final Random r) {
    super(r);
    this.numOfCycles = numOfCycles;
    this.thresholdForMisses = thresholdForMisses;
    this.roundsForDiffusion = roundsForDiffusion;
    this.roundsForAnswer = roundsForAnswer;
    lcm = new LogChunkMessages(numOfCycles, new Random(r.nextLong()));
    // LogChunkMessages.aspectOf().recordsToSave = numOfCycles;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    final long currentCycle = Utils.getRound();
    final Set<NodeAddress> currentNeighbors = alg.mainOverlay.getNeighbors();
    sentRequests.keySet().retainAll(currentNeighbors);
    sentRequestsCycle.keySet().retainAll(currentNeighbors);
    // Remove neighbors with pending verification requests for too long.
    final Set<NodeAddress> neighborsToRemove = new TreeSet<NodeAddress>();
    for (final Entry<NodeAddress, Long> entry : sentRequestsCycle.entrySet()) {
      if (entry.getValue() + roundsForAnswer < Utils.getRound()) {
        neighborsToRemove.add(entry.getKey());
        // System.out.println("Accusation sent in Omission defense due to request pending");
        DNVPAuditingIngredient.sendAccusation(sentRequests.get(entry.getKey()), entry.getKey(), alg.mainOverlay);
      }
    }
    sentRequests.keySet().removeAll(neighborsToRemove);
    sentRequestsCycle.keySet().removeAll(neighborsToRemove);
    // Update neighbors at this cycle
    cycleToNeighbors.put(currentCycle, new TreeSet<NodeAddress>(currentNeighbors));
    Utils.retainOnlyNewest(numOfCycles, cycleToNeighbors);
    // Update what I have at each round.
    if (!client.isServerMode() && client.player.getVs() != null) {
      cycleToMissingChunks.put(currentCycle,
          new TreeSet<Long>(client.player.getVs().getMissingChunks(alg.fromChunk, alg.latestChunk)));
      Utils.retainOnlyNewest(numOfCycles, cycleToMissingChunks);
    }
    // Choose a random neighbor's neighbor and send him a request regarding your
    // shared neighbor to check against omissions.
    final DNVPOverlayIngredient DNVP = (DNVPOverlayIngredient) alg.mainOverlay.getIngredient(DNVPOverlayIngredient.class);
    if (DNVP == null) {
      return;
    }
    final Map<NodeAddress, Set<NodeAddress>> neighborsNeighbors = DNVP.getNeighborsNeighbors();
    final Set<NodeAddress> neighborsToChooseFrom = new TreeSet<NodeAddress>();
    for (final NodeAddress neighbor : currentNeighbors) {
      if (!sentRequests.keySet().contains(neighbor) && neighborsNeighbors.containsKey(neighbor)
          && containsAnotherNeighborThatIsNotMe(neighborsNeighbors.get(neighbor))) {
        neighborsToChooseFrom.add(neighbor);
      }
    }
    if (neighborsToChooseFrom.size() > 0) {
      final NodeAddress neighbor = Utils.pickRandomElement(neighborsToChooseFrom, r);
      final NodeAddress neighborNeighbor = Utils.pickRandomElementExcept(neighborsNeighbors.get(neighbor),
          client.network.getAddress(), r);
      sentRequests.put(neighbor, neighborNeighbor);
      sentRequestsCycle.put(neighbor, Utils.getRound());
      client.network.send(new OmissionRequestMessage(getMessageTag(), client.network.getAddress(), neighborNeighbor, neighbor));
    }
  }
  
  private boolean containsAnotherNeighborThatIsNotMe(final Set<NodeAddress> set) {
    final Set<NodeAddress> clone = new TreeSet<NodeAddress>();
    clone.addAll(set);
    clone.remove(client.network.getAddress());
    return !clone.isEmpty();
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof OmissionRequestMessage) {
      handleOmissionRequestMessage((OmissionRequestMessage) message);
    }
    if (message instanceof BlameMessage) {
      handleBlameMessage((BlameMessage) message);
    }
    if (message instanceof DefendMessage) {
      handleDefendMessage((DefendMessage) message);
    }
    if (message instanceof OmissionResponseMessage) {
      handleOmissionResponseMessage((OmissionResponseMessage) message);
    }
  }
  
  private void handleOmissionRequestMessage(final OmissionRequestMessage message) {
    final NodeAddress nodeToCheck = message.nodeToCheck;
    // Ignore the request if nodeToCheck is no longer my neighbor.
    // if (!streaming.mainOverlay.getNeighbors().contains(nodeToCheck)) {
    // return;
    // }
    // Prepare sent chunks for sending.
    Map<Long, Set<Long>> sentChunks = null;
    if (lcm.sentChunks.containsKey(client.network.getAddress())) {
      sentChunks = lcm.sentChunks.get(client.network.getAddress()).get(nodeToCheck);
      if (sentChunks != null && sentChunks.size() == 0) {
        sentChunks = null;
      }
    }
    // Prepare missing chunks for sending.
    Map<Long, Set<Long>> missingChunks = new TreeMap<Long, Set<Long>>();
    for (final Entry<Long, Set<Long>> entry : cycleToMissingChunks.entrySet()) {
      if (cycleToNeighbors.containsKey(entry.getKey()) && cycleToNeighbors.get(entry.getKey()).contains(nodeToCheck)) {
        missingChunks.put(entry.getKey(), entry.getValue());
      }
    }
    if (missingChunks.size() == 0) {
      missingChunks = null;
    }
    client.network.send(new OmissionResponseMessage(getMessageTag(), client.network.getAddress(), message.sourceId,
        message.nodeToCheck, sentChunks, missingChunks));
  }
  
  private void handleBlameMessage(final BlameMessage message) {
    final NodeAddress nodeToCheck = message.nodeToCheck;
    sentRequests.remove(nodeToCheck);
    sentRequestsCycle.remove(nodeToCheck);
    DNVPAuditingIngredient.logMisbehavior(nodeToCheck, message.sourceId, alg.mainOverlay, getClass());
  }
  
  private void handleDefendMessage(final DefendMessage message) {
    // Do nothing except remove this pending omission request
    final NodeAddress nodeToCheck = message.nodeToCheck;
    sentRequests.remove(nodeToCheck);
    sentRequestsCycle.remove(nodeToCheck);
  }
  
  private void handleOmissionResponseMessage(final OmissionResponseMessage message) {
    final NodeAddress nodeToCheck = message.nodeToCheck;
    sentRequests.remove(nodeToCheck);
    sentRequestsCycle.remove(nodeToCheck);
    if (!alg.mainOverlay.getNeighbors().contains(nodeToCheck)) {
      return;
    }
    int countOfMisses = 0;
    // Check your missing chunks against the neighbor's neighbor sent chunks
    if (!client.isServerMode()) {
      for (final Entry<Long, Set<Long>> entry : message.sentChunks.entrySet()) {
        if (!cycleToMissingChunks.containsKey(entry.getKey())
            || !cycleToMissingChunks.containsKey(entry.getKey() + roundsForDiffusion)
            || !cycleToNeighbors.containsKey(entry.getKey()) || !cycleToNeighbors.containsKey(entry.getKey() + roundsForDiffusion)
            || !cycleToNeighbors.get(entry.getKey()).contains(nodeToCheck)
            || !cycleToNeighbors.get(entry.getKey() + roundsForDiffusion).contains(nodeToCheck)) {
          continue;
        }
        for (final Long chunkIndex : entry.getValue()) {
          if (cycleToMissingChunks.get(entry.getKey()).contains(chunkIndex)
              && cycleToMissingChunks.get(entry.getKey() + roundsForDiffusion).contains(chunkIndex)) {
            countOfMisses++;
            if (countOfMisses >= thresholdForMisses) {
              DNVPAuditingIngredient.logMisbehavior(nodeToCheck, message.sourceId, alg.mainOverlay, getClass());
              return;
            }
          }
        }
      }
    }
    // TODO make the sending of the missing chunks more efficient.
    // Check your sent chunks against the neighbor's neighbor missing chunks
    if (!lcm.sentChunks.containsKey(client.network.getAddress())
        || !lcm.sentChunks.get(client.network.getAddress()).containsKey(nodeToCheck)
        || lcm.sentChunks.get(client.network.getAddress()).get(nodeToCheck).size() == 0) {
      return;
    }
    final Map<Long, Set<Long>> sentChunksToNeighbor = lcm.sentChunks.get(client.network.getAddress()).get(nodeToCheck);
    final Set<Long> chunksMisses = new TreeSet<Long>();
    TextLogger.log(client.network.getAddress(), "sentChunksToNeighbor: " + sentChunksToNeighbor + "\n");
    final Set<Long> aggregatedSentChunksToNeighbor = new TreeSet<Long>();
    for (final Entry<Long, Set<Long>> entry : message.missingChunks.entrySet()) {
      aggregateSentChunks(aggregatedSentChunksToNeighbor, sentChunksToNeighbor, entry.getKey() - roundsForDiffusion);
      for (final Long chunkIndex : entry.getValue()) {
        if (aggregatedSentChunksToNeighbor.contains(chunkIndex) && !chunksMisses.contains(chunkIndex)) {
          countOfMisses++;
          chunksMisses.add(chunkIndex);
          if (countOfMisses >= thresholdForMisses) {
            // disconnectFromNeighbor(nodeToCheck, message.sourceId);
            DNVPAuditingIngredient.logMisbehavior(nodeToCheck, message.sourceId, alg.mainOverlay, getClass());
            return;
          }
        }
      }
    }
  }
  
  private static void aggregateSentChunks(final Set<Long> aggregatedSentChunksToNeighbor,
      final Map<Long, Set<Long>> sentChunksToNeighbor, final Long round) {
    for (final Entry<Long, Set<Long>> entry : sentChunksToNeighbor.entrySet()) {
      if (entry.getKey() <= round) {
        aggregatedSentChunksToNeighbor.addAll(entry.getValue());
      }
    }
  }
}
