package modules.overlays;

import ingredients.bootstrap.RandomSeedBootstrapIngredient;
import interfaces.Sizeable;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.AscendToTreeBoneMessage;
import messages.ConnectionRequestApprovedMessage;
import messages.ConnectionRequestForwardMessage;
import messages.ConnectionRequestMessage;
import messages.DisconnectMessage;
import messages.LoadQueryMessage;
import messages.LoadQueryResponseMessage;
import messages.Message;
import messages.SeedNodeSingleTargetReplyMessage;
import modules.P2PClient;
import utils.Common;
import utils.Utils;
import entites.SizeableLong;
import experiment.frameworks.NodeAddress;

public class TreeBone extends TreeOverlayModule<Long> {
  int distanceFromSource = -1;
  protected Set<NodeAddress> knownAncestors = new TreeSet<NodeAddress>();
  protected Map<NodeAddress, AncestorInfo> ancestorsInfo = new TreeMap<NodeAddress, AncestorInfo>();
  protected Map<NodeAddress, Long> pendingSons = new TreeMap<NodeAddress, Long>();
  private final int queryTimeout;
  int currQueryTime;
  private long upTime = 0;
  private final double stableCoefficient;
  private boolean stable = false;
  private final int freeSlots;
  RandomSeedBootstrapIngredient bootstrap;
  
  public TreeBone(final P2PClient client, final double stableCoefficient, final int queryTimeout, final int freeSlots,
      final Random r) {
    super(client, r);
    addIngredient(bootstrap = new RandomSeedBootstrapIngredient(AscendToTreeBoneMessage.class, new Random(r.nextLong())), client);
    this.stableCoefficient = stableCoefficient;
    this.queryTimeout = queryTimeout;
    this.freeSlots = freeSlots;
    currQueryTime = queryTimeout;
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    distanceFromSource = 0;
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof AscendToTreeBoneMessage) {
      assertServerStatus("non server in TreeBoneOverlay received AscendToTreeBoneMessage:" + network.getAddress());
    } else if (message instanceof ConnectionRequestMessage<?>) {
      if (getNeighbors().contains(message.sourceId) || !network.isUp(message.sourceId)) {
        return;
      }
      if (!getSonNodes().isEmpty()
          && (getSonNodes().size() + 1 + freeSlots) * Common.currentConfiguration.bitRate > network.getUploadBandwidth()) {
        // can't accept as a son
        final NodeAddress slowSon = Utils.findMinValueKey(neighborNodes);
        final long minUpload = getNeighborInfo(slowSon);
        if (minUpload < ((SizeableLong) ((ConnectionRequestMessage<?>) message).payload).l) {
          addSon(message.sourceId, ((SizeableLong) ((ConnectionRequestMessage<?>) message).payload).l);
          TextLogger.log(network.getAddress(), "adding " + message.sourceId + " removing " + slowSon + "\n");
          network.send(new ConnectionRequestForwardMessage(getMessageTag(), network.getAddress(), slowSon, message.sourceId));
          removeNeighbor(slowSon);
        } else {
          final NodeAddress neighbor = pickRedirectionNeighbor(message.sourceId);
          TextLogger.log(network.getAddress(), "redirecting " + message.sourceId + "\n");
          network.send(new ConnectionRequestForwardMessage(getMessageTag(), network.getAddress(), message.sourceId, neighbor));
        }
      } else {
        // add as a son
        addSon(message.sourceId, ((SizeableLong) ((ConnectionRequestMessage<?>) message).payload).l);
      }
    } else if (message instanceof ConnectionRequestForwardMessage) {
      if (message.sourceId.equals(fatherNode)) {
        TextLogger.log(network.getAddress(), "preempted \n");
        removeNeighbor(fatherNode);
        fatherNode = null;
      }
      if (fatherNode == null) {
        if (network.isUp(((ConnectionRequestForwardMessage) message).newNode)) {
          network.send(new ConnectionRequestMessage<SizeableLong>(getMessageTag(), network.getAddress(),
              ((ConnectionRequestForwardMessage) message).newNode, new SizeableLong((network.getUploadBandwidth()))));
        } else {
          findNewFatherNode();
        }
      }
    } else if (message instanceof ConnectionRequestApprovedMessage) {
      if (fatherNode != null && network.isUp(fatherNode) && !fatherNode.equals(message.sourceId)) {
        network.send(new DisconnectMessage(getMessageTag(), network.getAddress(), fatherNode));
        removeNeighbor(fatherNode);
        fatherNode = null;
      }
      knownAncestors.clear();
      ancestorsInfo.clear();
      knownAncestors.add(message.sourceId);
      final AncestorInfo ai = (AncestorInfo) ((ConnectionRequestApprovedMessage<?>) message).payload;
      distanceFromSource = ai.ancesotrDistanceFromSource + 1;
      ancestorsInfo.put(message.sourceId, ai);
      if (ai.father != null && !knownAncestors.contains(ai.father) && !ai.father.equals(network.getAddress())
          && network.isUp(ai.father)) {
        network.send(new LoadQueryMessage(getMessageTag(), network.getAddress(), ai.father));
        knownAncestors.add(ai.father);
      }
      addNeighbor(message.sourceId, ai.uploadBandwidth);
      fatherNode = message.sourceId;
      TextLogger.log(network.getAddress(), "father node is now " + fatherNode + "\n");
      updatePendingSons();
      updateSons();
    } else if (message instanceof LoadQueryMessage) {
      if (!network.isUp(message.sourceId)) {
        return;
      }
      final long usedBandwidth = calculateUsedUploadBandwidth();
      network.send(new LoadQueryResponseMessage(getMessageTag(), network.getAddress(), message.sourceId, usedBandwidth, network
          .getUploadBandwidth(), distanceFromSource, fatherNode));
    } else if (message instanceof LoadQueryResponseMessage) {
      final LoadQueryResponseMessage lqrm = (LoadQueryResponseMessage) message;
      if (message.sourceId.equals(fatherNode)) {
        if (distanceFromSource != lqrm.distanceFromSource + 1) {
          distanceFromSource = lqrm.distanceFromSource + 1;
          updateSons();
          cleanAncestors();
        }
      }
      final AncestorInfo ai = new AncestorInfo();
      ai.uploadBandwidth = lqrm.uploadBandwidth;
      ai.usedUploadBandwidth = lqrm.usedBandwidth;
      if (lqrm.fatherNode != null && !knownAncestors.contains(lqrm.fatherNode) && !lqrm.fatherNode.equals(network.getAddress())
          && network.isUp(lqrm.fatherNode)) {
        network.send(new LoadQueryMessage(getMessageTag(), network.getAddress(), lqrm.fatherNode));
        knownAncestors.add(lqrm.fatherNode);
      }
      ancestorsInfo.put(message.sourceId, ai);
    } else if (message instanceof DisconnectMessage) {
      removeNeighbor(message.sourceId);
      if (message.sourceId.equals(fatherNode)) {
        findNewFatherNode();
      }
    } else if (message instanceof SeedNodeSingleTargetReplyMessage) {
      if (fatherNode == null) {
        network.send(new ConnectionRequestMessage<SizeableLong>(getMessageTag(), network.getAddress(),
            ((SeedNodeSingleTargetReplyMessage) message).target, new SizeableLong((network.getUploadBandwidth()))));
      }
    }
  }
  
  private void updatePendingSons() {
    for (final Entry<NodeAddress, Long> entry : pendingSons.entrySet()) {
      final AncestorInfo ai = getMyInfo();
      if (ai == null) {
        throw new RuntimeException("updatePendingSons called with no distance to source!");
      }
      final NodeAddress son = entry.getKey();
      final Long upload = entry.getValue();
      addNeighbor(son, upload);
      network.send(new ConnectionRequestApprovedMessage<AncestorInfo>(getMessageTag(), network.getAddress(), son, ai));
    }
  }
  
  private void addSon(final NodeAddress son, final long upload) {
    final AncestorInfo ai = getMyInfo();
    if (ai != null) {
      addNeighbor(son, upload);
      network.send(new ConnectionRequestApprovedMessage<AncestorInfo>(getMessageTag(), network.getAddress(), son, ai));
      return;
    }
    pendingSons.put(son, upload);
  }
  
  private void cleanAncestors() {
    final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final Entry<NodeAddress, AncestorInfo> ancestor : ancestorsInfo.entrySet()) {
      if (!network.isUp(ancestor.getKey())) {
        toRemove.add(ancestor.getKey());
      }
      if (ancestor.getValue().ancesotrDistanceFromSource >= distanceFromSource) {
        toRemove.add(ancestor.getKey());
      }
    }
    ancestorsInfo.keySet().removeAll(toRemove);
    knownAncestors.removeAll(toRemove);
  }
  
  private void updateSons() {
    if (getNeighbors().size() > 1) {
      final long usedBandwidth = calculateUsedUploadBandwidth();
      for (final NodeAddress neighbor : getSonNodes()) {
        network.send(new LoadQueryResponseMessage(getMessageTag(), network.getAddress(), neighbor, usedBandwidth, network
            .getUploadBandwidth(), distanceFromSource, fatherNode));
      }
    }
  }
  
  private AncestorInfo getMyInfo() {
    if (distanceFromSource == -1) {
      return null;
    }
    final AncestorInfo ai = new AncestorInfo();
    ai.uploadBandwidth = network.getUploadBandwidth();
    ai.usedUploadBandwidth = calculateUsedUploadBandwidth();
    ai.ancesotrDistanceFromSource = distanceFromSource;
    ai.father = fatherNode;
    return ai;
  }
  
  private long calculateUsedUploadBandwidth() {
    return (getSonNodes().size()) * Common.currentConfiguration.bitRate;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (network.isServerMode()) {
      return;
    }
    if (fatherNode != null && !network.isUp(fatherNode)) {
      findNewFatherNode();
    }
    upTime++;
    // TODO add different computation for startup
    if (!stable && upTime >= (Common.currentConfiguration.playbackSeconds - Utils.getMovieTime() / 1000) * stableCoefficient) {
      stable = true;
      TextLogger.log(network.getAddress(), "is now stable\n");
    }
    if (stable) {
      performTransformations();
    }
  }
  
  private NodeAddress pickRedirectionNeighbor(final NodeAddress sourceId) {
    return Utils.pickRandomElementExcept(getSonNodes(), sourceId, r);
  }
  
  private void findNewFatherNode() {
    if (fatherNode != null) {
      ancestorsInfo.remove(fatherNode);
    }
    distanceFromSource = Integer.MAX_VALUE;
    TextLogger.log(network.getAddress(), "finding new father node!\n");
    fatherNode = null;
    if (!performLowDelayJump()) {
      bootstrap.initConnections();
    }
  }
  
  private void performTransformations() {
    sendQuery();
    // TODO maybe lowdelayjump first?
    if (!performHighDegreePreemption()) {
      performLowDelayJump();
    }
  }
  
  private void sendQuery() {
    if (knownAncestors.size() == 0) {
      // throw new RuntimeException("no known ancestors in sendQuery");
      // actually can happen - especially toward the end of the run
      return;
    }
    currQueryTime--;
    if (currQueryTime == 0) {
      currQueryTime = queryTimeout;
      NodeAddress n = null;
      while (!knownAncestors.isEmpty()) {
        n = Utils.pickRandomElement(knownAncestors, r);
        if (network.isUp(n)) {
          break;
        }
        knownAncestors.remove(n);
        n = null;
      }
      if (n != null) {
        network.send(new LoadQueryMessage(getMessageTag(), network.getAddress(), n));
      }
    }
  }
  
  private boolean performLowDelayJump() {
    int minDistance = distanceFromSource - 1;
    Entry<NodeAddress, AncestorInfo> chosenAncestor = null;
    for (final Entry<NodeAddress, AncestorInfo> ancestor : ancestorsInfo.entrySet()) {
      if (network.isUp(ancestor.getKey())
          && ancestor.getValue().uploadBandwidth - ancestor.getValue().usedUploadBandwidth > Common.currentConfiguration.bitRate
              * (1 + freeSlots) && ancestor.getValue().ancesotrDistanceFromSource < minDistance
          && !ancestor.getKey().equals(fatherNode)) {
        minDistance = ancestor.getValue().ancesotrDistanceFromSource;
        chosenAncestor = ancestor;
      }
    }
    if (chosenAncestor != null) {
      TextLogger.log(network.getAddress(), "performing Low Delay Jump\n");
      network.send(new ConnectionRequestMessage<SizeableLong>(getMessageTag(), network.getAddress(), chosenAncestor.getKey(),
          new SizeableLong((network.getUploadBandwidth()))));
      return true;
    }
    return false;
  }
  
  private boolean performHighDegreePreemption() {
    int minDistance = distanceFromSource;
    Entry<NodeAddress, AncestorInfo> chosenAncestor = null;
    for (final Entry<NodeAddress, AncestorInfo> ancestor : ancestorsInfo.entrySet()) {
      if (ancestor.getValue().uploadBandwidth < network.getUploadBandwidth()
          && ancestor.getValue().ancesotrDistanceFromSource < minDistance && ancestor.getValue().father != null) {
        minDistance = ancestor.getValue().ancesotrDistanceFromSource;
        chosenAncestor = ancestor;
      }
    }
    if (chosenAncestor != null) {
      TextLogger.log(network.getAddress(), "performing High Degree Preemption\n");
      network.send(new ConnectionRequestMessage<SizeableLong>(getMessageTag(), network.getAddress(),
          chosenAncestor.getValue().father, new SizeableLong((network.getUploadBandwidth()))));
      return true;
    }
    return false;
  }
  
  @Override public boolean isOverlayConnected() {
    return fatherNode != null && network.isUp(fatherNode);
  }
}

class AncestorInfo implements Serializable, Sizeable {
  @Override public String toString() {
    return "bandwidth:" + usedUploadBandwidth + "/" + uploadBandwidth + ", distanceFromSource=" + ancesotrDistanceFromSource
        + ", father=" + father + "]";
  }
  
  private static final long serialVersionUID = -2989746517859894092L;
  long uploadBandwidth;
  long usedUploadBandwidth;
  int ancesotrDistanceFromSource;
  NodeAddress father;
  
  @Override public long getSimulatedSize() {
    return NodeAddress.SIZE + Long.SIZE * 2 + Integer.SIZE;
  }
}