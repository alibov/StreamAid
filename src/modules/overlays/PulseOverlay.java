package modules.overlays;

import ingredients.HistoryCollectingIngredient;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.Message;
import messages.PULSE.WindowInfoMessage;
import modules.P2PClient;
import modules.player.VideoStream;
import utils.Common;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class PulseOverlay extends GroupedOverlayModule<Object> {
  private final HistoryCollectingIngredient HCB;
  public static final String missingGroupName = "MISSING";
  public static final String forwardGroupName = "FORWARD";
  private final SCAMP scampOverlay;
  private final int missingConnections;
  private final int forwardConnections;
  private final int roundsInEpoch;
  private int currentEpochTime = 0;
  private final LinkedList<Integer> TBinst = new LinkedList<Integer>();
  private static final int forwardAdditionPenalty = -3;
  private static final int serverThreshold = 5;
  Set<NodeAddress> infoSent = new TreeSet<NodeAddress>();
  Map<NodeAddress, BufferRange> neighborInfo = new HashMap<NodeAddress, BufferRange>();
  Map<NodeAddress, Long> nodeToTotalReceived = new HashMap<NodeAddress, Long>();
  Map<NodeAddress, Long> nodeToLastEpochDelta = new HashMap<NodeAddress, Long>();
  BufferRange myInfo = null;
  private final P2PClient client;
  
  public PulseOverlay(final P2PClient client, final int SCAMPconstant, final int missing, final int forward, final int epoch,
      final Random r) {
    super(client, r);
    if (client.player.bufferFromFirstChunk) {
      throw new RuntimeException("PulseOverlay is supposed to buffer from join time!");
    }
    if (!client.player.waitIfNoChunk) {
      throw new RuntimeException("PulseOverlay has to wait if no chunk!");
    }
    this.client = client;
    scampOverlay = new SCAMP(client, SCAMPconstant, new Random(r.nextLong()));
    missingConnections = missing;
    forwardConnections = forward;
    roundsInEpoch = epoch;
    currentEpochTime = roundsInEpoch;
    HCB = new HistoryCollectingIngredient(new Random(r.nextLong()));
    addIngredient(HCB, client);
    // TODO test updates
    HCB.ignoreList = getNeighbors();
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof WindowInfoMessage) {
      neighborInfo.put(message.sourceId, ((WindowInfoMessage) message).bufferRange);
      if (!infoSent.contains(message.sourceId)) {
        sendBufferInfo(message.sourceId);
      }
    }
  }
  
  private void sendBufferInfo(final NodeAddress neighbor) {
    network.send(new WindowInfoMessage(getMessageTag(), network.getAddress(), neighbor, getBufferRange()));
    infoSent.add(neighbor);
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    scampOverlay.nextCycle();
    if (client.player.getVs() != null && client.player.getVs().getLatestChunk() != null) {
      TBinst
      .add((int) (Utils.getMovieTime() / Common.currentConfiguration.cycleLength - client.player.getVs().windowOffset + VideoStream.startingFrame));
      if (TBinst.size() > roundsInEpoch) {
        TBinst.removeFirst();
      }
    }
    if (!scampOverlay.isOverlayConnected()) {
      return;
    }
    currentEpochTime--;
    if (currentEpochTime == 1) {
      for (final NodeAddress neighbor : scampOverlay.getNeighbors()) {
        sendBufferInfo(neighbor);
      }
    } else if (currentEpochTime == 0) {
      currentEpochTime = roundsInEpoch;
      removeNeighbors(getNeighbors());
      for (final NodeAddress n : network.sendInfo.keySet()) {
        Utils.checkExistence(nodeToTotalReceived, n, 0L);
        final long newTotal = network.sendInfo.get(n).contentBitsReceived;
        nodeToLastEpochDelta.put(n, newTotal - nodeToTotalReceived.get(n));
        nodeToTotalReceived.put(n, newTotal);
      }
    }
    if (!network.isServerMode()) {
      fillMissingGroup();
      fillForwardGroup();
    } else if (getNeighbors().isEmpty()) {
      chooseNodesForServer();
    }
  }
  
  private void chooseNodesForServer() {
    // choosing random nodes that have average lag lower than set threshold
    List<NodeAddress> candidates = new LinkedList<NodeAddress>();
    for (final NodeAddress cand : neighborInfo.keySet()) {
      if (neighborInfo.get(cand).averageLag < serverThreshold) {
        candidates.add(cand);
      }
    }
    Collections.shuffle(candidates, r);
    candidates = candidates.subList(0,
        Math.min((int) network.getUploadBandwidth() / Common.currentConfiguration.bitRate, candidates.size()));
    addToGroup(missingGroupName, candidates);
    candidates = new LinkedList<NodeAddress>();
    candidates.addAll(scampOverlay.getNeighbors());
    candidates.removeAll(getNodeGroup(missingGroupName));
    Collections.shuffle(candidates, r);
    if (network.getUploadBandwidth() != Long.MAX_VALUE) {
      candidates = candidates.subList(
          0,
          Math.min((int) (network.getUploadBandwidth() / Common.currentConfiguration.bitRate)
              - getNodeGroup(missingGroupName).size(), candidates.size()));
    }
    addToGroup(missingGroupName, candidates);
  }
  
  private void fillForwardGroup() {
    // find peers for FORWARD
    if (getNodeGroup(forwardGroupName).size() < forwardConnections) {
      final LinkedHashMap<NodeAddress, Integer> sortedScores = Utils.sortByValue(HCB.historyScores);
      sortedScores.keySet().removeAll(getNodeGroup(missingGroupName));
      sortedScores.keySet().remove(network.getServerNode());
      for (final NodeAddress cand : sortedScores.keySet()) {
        if (getNodeGroup(forwardGroupName).size() >= forwardConnections) {
          break;
        }
        // must be non overlapping and farther
        if (!neighborInfo.containsKey(cand) || neighborInfo.get(cand).averageLag >= getBufferRange().oldestLag) {
          continue;
        }
        // missing group members are ignored
        if (getNodeGroup(missingGroupName).contains(cand)) {
          continue;
        }
        addToGroup(forwardGroupName, cand);
        HCB.addToScore(cand, forwardAdditionPenalty);
      }
    }
  }
  
  private void fillMissingGroup() {
    // find peers for MISSING
    if (getNodeGroup(missingGroupName).size() < missingConnections) {
      final LinkedHashMap<NodeAddress, Long> sorted = Utils.sortByValue(nodeToLastEpochDelta);
      // adding nodes that sent chunks during last epoch
      for (final NodeAddress cand : sorted.keySet()) {
        if (getNodeGroup(missingGroupName).size() >= missingConnections) {
          break;
        }
        addToGroup(missingGroupName, cand);
      }
      // fill the rest of the quota with peers with most overlapping window
      if (getNodeGroup(missingGroupName).size() < missingConnections) {
        final Map<NodeAddress, Integer> overlap = new TreeMap<NodeAddress, Integer>();
        final BufferRange myRange = getBufferRange();
        for (final NodeAddress n : neighborInfo.keySet()) {
          overlap.put(n, myRange.computeOverlap(neighborInfo.get(n)));
        }
        final LinkedHashMap<NodeAddress, Integer> sortedOverlap = Utils.sortByValue(overlap);
        for (final NodeAddress cand : sortedOverlap.keySet()) {
          if (getNodeGroup(missingGroupName).size() >= missingConnections) {
            break;
          }
          addToGroup(missingGroupName, cand);
        }
      }
    }
  }
  
  @Override public boolean isOverlayConnected() {
    if (!network.isServerMode()) {
      return !getNeighbors().isEmpty();
    }
    return super.isOverlayConnected();
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    scampOverlay.setServerMode();
  }
  
  BufferRange getBufferRange() {
    final BufferRange retVal = new BufferRange();
    if (client.player.getVs() == null) {
      return retVal;
    }
    retVal.oldestLag = (int) (Utils.getMovieTime() / Common.currentConfiguration.cycleLength
        - client.player.getVs().getEarliestChunk() + VideoStream.startingFrame);
    if (TBinst.isEmpty()) {
      retVal.averageLag = retVal.oldestLag;
      return retVal;
    }
    retVal.averageLag = Utils.averageInt(TBinst);
    return retVal;
  }
  
  public class BufferRange {
    public int averageLag;
    public int oldestLag;
    
    @Override public String toString() {
      return "" + oldestLag + "-" + averageLag;
    }
    
    public int computeOverlap(final BufferRange other) {
      return Math.max(0, Math.min(averageLag, other.averageLag) - Math.max(oldestLag, other.oldestLag));
    }
  }
}
