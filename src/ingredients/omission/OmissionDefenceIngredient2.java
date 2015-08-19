package ingredients.omission;

import ingredients.AbstractIngredient;
import ingredients.LogReceivedChunksIngredient;
import ingredients.DNVP.DNVPOverlayIngredient;
import ingredients.network.LogChunkMessages;
import ingredients.network.LogChunkRequestMessage;
import ingredients.overlay.NeighborChunkAvailabilityIngredient;
import ingredients.overlay.security.FreeridingAuditingIngredient;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.Message;
import messages.OmissionDefense.OmissionRequestMessage;
import messages.OmissionDefense.OmissionResponseMessage;
import modules.P2PClient;
import modules.streaming.StreamingModule;
import utils.Common;
import utils.Utils;
import entites.Availability;
import experiment.frameworks.NodeAddress;

public class OmissionDefenceIngredient2 extends AbstractIngredient<StreamingModule> {
  private final LogReceivedChunksIngredient logReceivedChunks;
  private final Map<Long/* cycle */, Map<NodeAddress/* neighbor */, Availability>> neighborsAvailability = new TreeMap<Long, Map<NodeAddress, Availability>>();
  private final Map<Long, Set<NodeAddress>> cycleToNeighbors = new TreeMap<Long, Set<NodeAddress>>();
  // A mapping between a neighbor and its neighbor neighbors who received a
  // request from this node. In case there is no answer from any of those
  // neighbor
  // neighbors for several rounds we need act accordingly.
  private final Map<NodeAddress // my neighbor
  , Map<NodeAddress // the request receiver
  , Long // the cycle the request sent
  >> sentRequests = new TreeMap<NodeAddress, Map<NodeAddress, Long>>();
  private final int numOfCycles;
  private final int thresholdForMisses;
  private final int roundsForBitmap;
  private final int roundsForAnswer;
  private final int verificationsPerCycle;
  private final LogChunkMessages lcm;
  private final LogChunkRequestMessage lcrm;
  
  @Override public void setClientAndComponent(final P2PClient client, final StreamingModule alg) {
    super.setClientAndComponent(client, alg);
    alg.addIngredient(logReceivedChunks, client);
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    logReceivedChunks.setServerMode();
  }
  
  public OmissionDefenceIngredient2(final int numOfCycles, final int thresholdForMisses, final int roundsForBitmap,
      final int roundsForAnswer, final int verificationsPerCycle, final Random r) {
    super(r);
    this.numOfCycles = numOfCycles;
    this.thresholdForMisses = thresholdForMisses;
    this.roundsForBitmap = roundsForBitmap;
    this.roundsForAnswer = roundsForAnswer;
    this.verificationsPerCycle = verificationsPerCycle;
    lcm = new LogChunkMessages(numOfCycles, new Random(r.nextLong()));
    lcrm = new LogChunkRequestMessage(numOfCycles, new Random(r.nextLong()));
    logReceivedChunks = new LogReceivedChunksIngredient(new Random(r.nextLong()));
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    // If I'm the source don't do anything
    if (client.isServerMode()) {
      return;
    }
    final long currentCycle = Utils.getRound();
    final Set<NodeAddress> currentNeighbors = alg.mainOverlay.getNeighbors();
    sentRequests.keySet().retainAll(currentNeighbors);
    // Remove neighbors with pending verification requests for too long.
    final Set<NodeAddress> neighborNeighborsToRemove = new TreeSet<NodeAddress>();
    for (final Entry<NodeAddress, Map<NodeAddress, Long>> entry : sentRequests.entrySet()) {
      for (final Entry<NodeAddress, Long> entry2 : entry.getValue().entrySet()) {
        if (entry2.getValue() + roundsForAnswer < Utils.getRound()) {
          neighborNeighborsToRemove.add(entry2.getKey());
          TextLogger.log(client.network.getAddress(), "Accusation sent in Omission defense due to request pending" + " via "
              + this.getClass().getSimpleName() + "\n");
          FreeridingAuditingIngredient.sendAccusation(entry2.getKey(), entry.getKey(), alg.mainOverlay);
        }
      }
      entry.getValue().keySet().removeAll(neighborNeighborsToRemove);
    }
    // Update neighbors at this cycle
    cycleToNeighbors.put(currentCycle, new TreeSet<NodeAddress>(currentNeighbors));
    Utils.retainOnlyNewest(numOfCycles * 3, cycleToNeighbors);
    // Update my neighbors availability at the current cycle
    final NeighborChunkAvailabilityIngredient NCAB = (NeighborChunkAvailabilityIngredient) alg.mainOverlay
        .getIngredient(NeighborChunkAvailabilityIngredient.class);
    if (NCAB != null) {
      final Map<NodeAddress, Availability> currentNeighborsAvailability = new TreeMap<NodeAddress, Availability>();
      for (final Entry<NodeAddress, Availability> entry : NCAB.neighborAvailability.entrySet()) {
        currentNeighborsAvailability.put(entry.getKey(), new Availability(entry.getValue()));
      }
      neighborsAvailability.put(currentCycle, currentNeighborsAvailability);
      Utils.retainOnlyNewest(numOfCycles * 3, neighborsAvailability);
    }
    // Perform a local check for all of my neighbors.
    final Set<NodeAddress> failedLocalCheckNeighbors = new TreeSet<NodeAddress>();
    for (final NodeAddress neighbor : currentNeighbors) {
      if (!localCheck(neighbor)) {
        System.out.println("===========Local check failed================");
        failedLocalCheckNeighbors.add(neighbor);
        // throw new
        // IllegalStateException("Not supposed to fail the local check with no chunks dropping.");
      }
    }
    for (final NodeAddress failedLocalCheckNeighbor : failedLocalCheckNeighbors) {
      FreeridingAuditingIngredient.logMisbehavior(failedLocalCheckNeighbor, client.network.getAddress(), alg.mainOverlay,
          getClass());
    }
    // Choose 'verificationsPerCycle' random neighbors' neighbors and send them
    // a request regarding your shared neighbor with them to check against
    // omissions.
    sendOmissionRequestMessages();
  }
  
  private void sendOmissionRequestMessages() {
    final DNVPOverlayIngredient DNVP = (DNVPOverlayIngredient) alg.mainOverlay.getIngredient(DNVPOverlayIngredient.class);
    if (DNVP == null) {
      return;
    }
    final Set<NodeAddress> currentNeighbors = alg.mainOverlay.getNeighbors();
    final Map<NodeAddress, Set<NodeAddress>> neighborsNeighbors = DNVP.getNeighborsNeighbors();
    final Set<NodeAddress> neighborsToChooseFrom = new TreeSet<NodeAddress>();
    for (final NodeAddress neighbor : currentNeighbors) {
      if (!sentRequests.keySet().contains(neighbor) && neighborsNeighbors.containsKey(neighbor)
          && containsAnotherNeighborThatIsNotMe(neighborsNeighbors.get(neighbor))) {
        neighborsToChooseFrom.add(neighbor);
      }
    }
    for (int i = 0; i < verificationsPerCycle; i++) {
      if (neighborsToChooseFrom.size() <= 0) {
        break;
      }
      final NodeAddress neighbor = Utils.pickRandomElement(neighborsToChooseFrom, r);
      final NodeAddress neighborNeighbor = Utils.pickRandomElementExcept(neighborsNeighbors.get(neighbor),
          client.network.getAddress(), r);
      Utils.checkExistence(sentRequests, neighbor, new TreeMap<NodeAddress, Long>());
      sentRequests.get(neighbor).put(neighborNeighbor, Utils.getRound());
      client.network.send(new OmissionRequestMessage(getMessageTag(), client.network.getAddress(), neighborNeighbor, neighbor));
      neighborsToChooseFrom.remove(neighbor);
    }
  }
  
  /**
   * Performs a local check of the requests sent to this node against the chunk
   * received from this node.
   * 
   * @return true if the check succeeded, false otherwise.
   */
  private boolean localCheck(final NodeAddress neighbor) {
    assert !client.isServerMode();
    if (!lcrm.sentRequests.containsKey(client.network.getAddress())
        || !lcrm.sentRequests.get(client.network.getAddress()).containsKey(neighbor)
        || lcrm.sentRequests.get(client.network.getAddress()).get(neighbor).size() == 0) {
      // TODO remove this debugging code
      // final int myGroup =
      // Common.currentConfiguration.getNodeGroup(client.node.getImpl().toString());
      // final int nodeToCheckGroup =
      // Common.currentConfiguration.getNodeGroup(neighbor.toString());
      // if (/* myGroup == 0 && */nodeToCheckGroup == 1) {
      // throw new IllegalStateException("No request from " +
      // client.node.getImpl() + " to " + neighbor);
      // System.out.println("No request sent to " + neighbor);
      // }
      return true;
    }
    final Map<Long, Set<Long>> sentRequestsToNeighbor = lcrm.sentRequests.get(client.network.getAddress()).get(neighbor);
    final Map<Long, Set<Long>> receivedChunksFromNeighbor = logReceivedChunks.receivedChunks.get(neighbor);
    final Set<Long> chunksMisses = new TreeSet<Long>();
    // TextLogger.log(client.node.getImpl(), "sentChunksToNeighbor: " +
    // sentChunksToNeighbor + "\n");
    final Set<Long> aggregatedReceivedChunksFromNeighbor = new TreeSet<Long>();
    // Checking that for every request sent to this neighbor I got an answer in
    // "roundsForAnswer"
    for (final Entry<Long, Set<Long>> entry : sentRequestsToNeighbor.entrySet()) {
      if (entry.getKey() + roundsForAnswer > Utils.getRound()) {
        continue;
      }
      aggregateReceivedChunks(aggregatedReceivedChunksFromNeighbor, receivedChunksFromNeighbor, entry.getKey() + roundsForAnswer);
      for (final Long chunkIndex : entry.getValue()) {
        if (!aggregatedReceivedChunksFromNeighbor.contains(chunkIndex) && !chunksMisses.contains(chunkIndex)) {
          chunksMisses.add(chunkIndex);
          System.out.println("Chunk that wasn't delivered is: " + chunkIndex);
          if (chunksMisses.size() >= thresholdForMisses) {
            return false;
          }
        }
      }
    }
    // TODO remove this debugging code
    // final int myGroup =
    // Common.currentConfiguration.getNodeGroup(client.node.getImpl().toString());
    final int nodeToCheckGroup = Common.currentConfiguration.getNodeGroup(neighbor.toString());
    if (/* myGroup == 0 && */nodeToCheckGroup == 1) {
      // throw new IllegalStateException("No good");
      // System.out.println("No Good");
    }
    return true;
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
    if (message instanceof OmissionResponseMessage) {
      handleOmissionResponseMessage((OmissionResponseMessage) message);
    }
  }
  
  // TODO remove the sending of the missing chunks and add an accusation in case
  // of omission
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
    final Map<Long, Set<Long>> missingChunks = null;
    client.network.send(new OmissionResponseMessage(getMessageTag(), client.network.getAddress(), message.sourceId,
        message.nodeToCheck, sentChunks, missingChunks));
  }
  
  // TODO Log the messages actually before sending, and not before the send
  // function cause there might not be sufficient upload bandwidth.
  private void handleOmissionResponseMessage(final OmissionResponseMessage message) {
    // TODO remove this debugging code
    // final int myGroup =
    // Common.currentConfiguration.getNodeGroup(client.node.getImpl().toString());
    // final int nodeToCheckGroup =
    // Common.currentConfiguration.getNodeGroup(message.nodeToCheck.toString());
    // final int sourceGroup =
    // Common.currentConfiguration.getNodeGroup(message.sourceId.toString());
    // if (nodeToCheckGroup == 1 && message.sentChunks.entrySet().size() > 0) {
    // System.out.println();
    // }
    if (client.isServerMode()) {
      throw new IllegalStateException("Server got omission response message");
    }
    final NodeAddress nodeToCheck = message.nodeToCheck;
    if (sentRequests.containsKey(nodeToCheck)) {
      sentRequests.get(nodeToCheck).remove(message.sourceId);
    }
    if (!alg.mainOverlay.getNeighbors().contains(nodeToCheck)) {
      return;
    }
    // Check the chunks sent by the neighbor's neighbor to my neighbor against
    // the received bitmaps from my neighbor.
    int countOfMisses = 0;
    for (final Entry<Long, Set<Long>> entry : message.sentChunks.entrySet()) {
      boolean isNeighborForLongEnough = true;
      for (Long cycle = entry.getKey(); cycle <= (entry.getKey() + roundsForBitmap); cycle++) {
        if (!cycleToNeighbors.containsKey(cycle) || !cycleToNeighbors.get(cycle).contains(nodeToCheck)
        /* || !neighborsAvailability.containsKey(entry.getKey() +
         * roundsForBitmap) || !neighborsAvailability.get(entry.getKey() +
         * roundsForBitmap).containsKey(nodeToCheck) */) {
          isNeighborForLongEnough = false;
        }
      }
      if (!isNeighborForLongEnough) {
        continue;
      }
      for (final Long chunkIndex : entry.getValue()) {
        if (!neighborsAvailability.containsKey(entry.getKey() + roundsForBitmap)
            || !neighborsAvailability.get(entry.getKey() + roundsForBitmap).containsKey(nodeToCheck)) {
          countOfMisses++;
          if (countOfMisses >= thresholdForMisses) {
            FreeridingAuditingIngredient.logMisbehavior(nodeToCheck, message.sourceId, alg.mainOverlay, getClass());
            return;
          }
        }
        if (!neighborsAvailability.get(entry.getKey() + roundsForBitmap).get(nodeToCheck).hasChunk(chunkIndex)) {
          // throw new IllegalStateException("Bitmap glitch in chunk: " +
          // chunkIndex + " ; " + message.sourceId + ","
          // + message.nodeToCheck + "," + message.destID);
          countOfMisses++;
          if (countOfMisses >= thresholdForMisses) {
            FreeridingAuditingIngredient.logMisbehavior(nodeToCheck, message.sourceId, alg.mainOverlay, getClass());
            return;
          }
        }
      }
    }
  }
  
  private static void aggregateReceivedChunks(final Set<Long> aggregatedReceivedChunksFromNeighbor,
      final Map<Long, Set<Long>> receivedChunksFromNeighbor, final Long round) {
    if (receivedChunksFromNeighbor == null) {
      return;
    }
    for (final Entry<Long, Set<Long>> entry : receivedChunksFromNeighbor.entrySet()) {
      if (entry.getKey() <= round) {
        aggregatedReceivedChunksFromNeighbor.addAll(entry.getValue());
      }
    }
  }
}
