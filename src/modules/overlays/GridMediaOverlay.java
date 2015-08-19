package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;
import interfaces.Sizeable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;

import messages.AliveMessage;
import messages.ConnectionRequestApprovedMessage;
import messages.Message;
import messages.PartialMembershipViewMessage;
import messages.SeedNodeMultipleTargetsReplyMessage;
import modules.P2PClient;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class GridMediaOverlay extends OverlayModule<MemebrInfo> {
  // TODO remove member table
  Map<NodeAddress, MemebrInfo> memberTable = new TreeMap<NodeAddress, MemebrInfo>();
  private final int alivePeriod = 2;
  private final int gossipPeriod = 6;
  private final int minLatencyNeighborSize;
  private final int neighborSize;
  private final int gossipSize;
  public final int lifeTimeThreshold = 5000;
  
  public GridMediaOverlay(final P2PClient client, final int candidateListSize, final int neighborSize,
      final int minLatencyNeighborSize, final int gossipSize, final Random r) {
    super(client, r);
    addIngredient(new RandomGroupBootstrapIngredient(candidateListSize, new Random(r.nextLong())), client);
    this.neighborSize = neighborSize;
    this.minLatencyNeighborSize = minLatencyNeighborSize;
    this.gossipSize = gossipSize;
  }
  
  protected void handleSeedNodeMultipleTargetsReplyMessage(final SeedNodeMultipleTargetsReplyMessage message) {
    chooseNeighborsFromMemberTable(message.targets, minLatencyNeighborSize);
  }
  
  protected void handleConnectionRequestApprovedMessage(final ConnectionRequestApprovedMessage<?> message) {
    addNeighbor(message.sourceId);
  }
  
  private void chooseNeighborsFromMemberTable(final Collection<NodeAddress> possibleTargets, final int minLatencyNeighborSizeParam) {
    final Map<NodeAddress, Long/* latency */> latencyMap = new TreeMap<NodeAddress, Long>();
    for (final NodeAddress target : possibleTargets) {
      latencyMap.put(target, network.getEstimatedLatency(target));
    }
    final LinkedHashMap<NodeAddress, Long> sortedMap = Utils.sortByValue(latencyMap);
    int i = 0;
    final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
    // find minLatencyNeighborSize neighbors with min latency
    for (final NodeAddress neighbor : sortedMap.keySet()) {
      toRemove.add(neighbor);
      if (!network.isUp(neighbor)) {
        continue;
      }
      final MemebrInfo mi = new MemebrInfo();
      mi.aliveTime = alivePeriod;
      mi.gossipTime = gossipPeriod;
      memberTable.put(neighbor, mi);
      if (i < minLatencyNeighborSizeParam) {
        addNeighbor(neighbor);
        sendMessage(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), neighbor));
      }
      i++;
    }
    latencyMap.keySet().removeAll(toRemove);
    // the rest of the neighbors are chosen randomly from the targets
    final ArrayList<NodeAddress> seedNodes = new ArrayList<NodeAddress>(latencyMap.keySet());
    NodeAddress target = null;
    final List<NodeAddress> chosenNodes = new LinkedList<NodeAddress>();
    Collections.shuffle(seedNodes, r);
    while (chosenNodes.size() < neighborSize - i && !seedNodes.isEmpty()) {
      target = seedNodes.get(seedNodes.size() - 1);
      if (network.isUp(target)) {
        chosenNodes.add(target);
      } else {
        memberTable.keySet().remove(target);
      }
      seedNodes.remove(target);
    }
    for (final NodeAddress neighbor : seedNodes) {
      addNeighbor(neighbor);
      network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), neighbor));
    }
  }
  
  void sendMessage(final Message m) {
    if (memberTable.containsKey(m.destID)) {
      memberTable.get(m.destID).sentBits += Utils.getSize(m);
    }
    network.send(m);
  }
  
  @Override public void handleMessage(final Message message) {
    if (memberTable.containsKey(message.sourceId)) {
      memberTable.get(message.sourceId).receivedBits += Utils.getSize(message);
      memberTable.get(message.sourceId).lastMessageTime = Utils.getTime();
    }
    super.handleMessage(message);
    if (message instanceof PartialMembershipViewMessage) {
      final Set<NodeAddress> neighborsList = ((PartialMembershipViewMessage<?>) message).infoMap.keySet();
      // add the neighbors received in the message
      for (final NodeAddress newNeighbor : neighborsList) {
        final MemebrInfo mi = new MemebrInfo();
        mi.aliveTime = alivePeriod;
        mi.gossipTime = gossipPeriod;
        memberTable.put(newNeighbor, mi);
      }
    } else if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      handleSeedNodeMultipleTargetsReplyMessage((SeedNodeMultipleTargetsReplyMessage) message);
    } else if (message instanceof ConnectionRequestApprovedMessage) {
      handleConnectionRequestApprovedMessage((ConnectionRequestApprovedMessage<?>) message);
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
    // update member table, send alive messages
    for (final NodeAddress member : memberTable.keySet()) {
      final MemebrInfo ni = memberTable.get(member);
      if (Utils.getTime() - ni.lastMessageTime > lifeTimeThreshold) {
        toRemove.add(member);
        continue;
      }
      ni.aliveTime--;
      if (ni.aliveTime == 0) {
        ni.aliveTime = alivePeriod;
        sendMessage(new AliveMessage(getMessageTag(), network.getAddress(), member));
      }
    }
    memberTable.keySet().removeAll(toRemove);
    removeNeighbors(toRemove);
    // gossip
    for (final NodeAddress neighbor : getNeighbors()) {
      final MemebrInfo ni = memberTable.get(neighbor);
      ni.gossipTime--;
      if (ni.gossipTime == 0) {
        ni.gossipTime = gossipPeriod;
        final List<NodeAddress> memberList = new ArrayList<NodeAddress>(memberTable.keySet());
        memberList.remove(neighbor);
        Collections.shuffle(memberList, r);
        final List<NodeAddress> sublist = memberList.subList(0, Math.min(gossipSize, memberList.size()));
        sendMessage(new PartialMembershipViewMessage<Sizeable>(getMessageTag(), network.getAddress(), neighbor,
            new LinkedList<NodeAddress>(sublist)));
      }
    }
    // replace fallen neighbors from member view
    final Set<NodeAddress> neighbors = getNeighbors();
    if (neighbors.size() < neighborSize && neighborSize < memberTable.size()) {
      final TreeMap<NodeAddress, MemebrInfo> addCandidates = new TreeMap<NodeAddress, MemebrInfo>(memberTable);
      addCandidates.keySet().removeAll(getNeighbors());
      final Set<NodeAddress> minVal = Utils.findMinValueKeyGroup(addCandidates);
      for (final NodeAddress n : minVal) {
        if (neighbors.size() < neighborSize) {
          addNeighbor(n);
          network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), n));
        }
      }
    }
  }
}

class MemebrInfo implements Comparable<MemebrInfo> {
  long sentBits = 0;
  long receivedBits = 0;
  long lastMessageTime;
  int aliveTime;
  int gossipTime;
  
  @Override public int compareTo(final MemebrInfo o) {
    return new Long(sentBits + receivedBits).compareTo(o.sentBits + o.receivedBits);
  }
}