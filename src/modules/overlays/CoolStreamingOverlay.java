package modules.overlays;

import interfaces.Sizeable;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import logging.TextLogger;
import messages.ConnectionRequestApprovedMessage;
import messages.ConnectionRequestDeclinedMessage;
import messages.ConnectionRequestMessage;
import messages.DeputyRequestMessage;
import messages.MembershipMessage;
import messages.Message;
import messages.PartialMembershipViewMessage;
import messages.SeedNodeMultipleTargetsReplyMessage;
import messages.SeedNodeSingleTargetReplyMessage;
import modules.P2PClient;
import modules.network.NetworkModule.SendInfo;
import utils.Common;
import utils.Utils;
import experiment.frameworks.NodeAddress;

public class CoolStreamingOverlay extends OverlayModule<partnerInfo> {
  private final SCAMP scampOverlay;
  private final int M; // number of partners
  private final int H; // max number of partners
  private final int exploreRound;
  private final int membershipGossipTimeout;
  private int currentGossipTime = 1;
  private final int amountToSend;
  private final TreeMap<NodeAddress, memberInfo> mCache = new TreeMap<NodeAddress, memberInfo>();
  
  public CoolStreamingOverlay(final P2PClient client, final int SCAMPconstant, final int M, final int H, final int amountToSend,
      final int exploreRound, final int gossipTimeout, final Random r) {
    super(client, r);
    scampOverlay = new SCAMP(client, SCAMPconstant, new Random(r.nextLong()));
    this.M = M;
    this.H = H;
    this.amountToSend = amountToSend;
    this.exploreRound = exploreRound;
    membershipGossipTimeout = gossipTimeout;
  }
  
  @Override public void setConfNumber(final int confNumber) {
    super.setConfNumber(confNumber);
    scampOverlay.setConfNumber(confNumber);
  }
  
  @Override public void deactivate() {
    super.deactivate();
    scampOverlay.deactivate();
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof DeputyRequestMessage) {
      // get the nodes that i have contact with (my partners) and send them back
      // in a random order to the source
      List<NodeAddress> subList = new LinkedList<NodeAddress>(mCache.keySet());
      subList.remove(message.sourceId);
      if (subList.isEmpty()) {
        TextLogger.log(network.getAddress(), "no nodes in mCache, returning\n");
        return;
      }
      Collections.shuffle(subList, r);
      subList = new LinkedList<NodeAddress>(subList.subList(0, Math.min(subList.size() - 1, H - 1)));
      network.send(new SeedNodeMultipleTargetsReplyMessage(getMessageTag(), network.getAddress(), message.sourceId, subList));
    } else if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      // when I got the list send contact request to all of them
      for (final NodeAddress n : ((SeedNodeMultipleTargetsReplyMessage) message).targets) {
        network.send(new ConnectionRequestMessage<Sizeable>(getMessageTag(), network.getAddress(), n));
      }
    } else if (message instanceof ConnectionRequestMessage) {
      if (!network.isUp(message.sourceId)) {
        return;
      }
      // if I have less then H neighbors then add the source has neighbors, else
      // decline
      if (getNeighbors().size() < H) {
        network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), network.getAddress(), message.sourceId));
        addNeighbor(message.sourceId, new partnerInfo());
        return;
      }
      network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), network.getAddress(), message.sourceId));
    } else if (message instanceof ConnectionRequestApprovedMessage) {
      // my request was approved, if I have less then H neighbors then add
      // neighbor
      if (getNeighbors().size() >= H) {
        network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), network.getAddress(), message.sourceId));
        return;
      }
      addNeighbor(message.sourceId, new partnerInfo());
    } else if (message instanceof ConnectionRequestDeclinedMessage) {
      // if I got refused then remove him from neighbors, and if I don't have
      // enough neighbors then request random member in the cache the become my
      // neighbors
      removeNeighbor(message.sourceId);
      refillNeighbors();
    } else if (message instanceof MembershipMessage) {
      // if I don't have a newer entry in the cache from this source then add
      // this source to the cache
      // also update the cache
      final MembershipMessage msg = (MembershipMessage) message;
      if (mCache.containsKey(msg.id) && mCache.get(msg.id).seq_num >= msg.seq_num) {
        return;
      }
      updatemCache();
      mCache.put(msg.id, new memberInfo(msg));
    } else if (message instanceof PartialMembershipViewMessage) {
      final Map<NodeAddress, ?> neighborsList = ((PartialMembershipViewMessage<?>) message).infoMap;
      // add the neighbors received in the message
      for (final NodeAddress newNeighbor : neighborsList.keySet()) {
        mCache.put(newNeighbor, (memberInfo) neighborsList.get(newNeighbor));
      }
    } else if (message instanceof SeedNodeSingleTargetReplyMessage) {
      // send request to the seed to get list from which I will get my neighbors
      network.send(new DeputyRequestMessage(getMessageTag(), network.getAddress(),
          ((SeedNodeSingleTargetReplyMessage) message).target));
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    scampOverlay.nextCycle();
    if (!scampOverlay.isOverlayConnected()) {
      return;
    }
    updatemCache();
    if (currentGossipTime > 0) {
      currentGossipTime--;
    }
    if (currentGossipTime == 0) {
      currentGossipTime = membershipGossipTimeout;
      GossipingOverlay.sendGossipMessages(this, getNeighbors(), mCache, amountToSend, r);
      for (final NodeAddress n : scampOverlay.getNeighbors()) {
        network.send(new MembershipMessage(getMessageTag(), network.getAddress(), n, secondsUp, getNeighbors().size(),
            Common.currentConfiguration.cycleLength * membershipGossipTimeout, network.getAddress()));
      }
    }
    if (secondsUp % exploreRound == 0 && !network.isServerMode()) {
      // server mode does not do this
      if (getNeighbors().size() > M) {
        final Map<NodeAddress, Long> neighborStats = new HashMap<NodeAddress, Long>();
        for (final NodeAddress n : getNeighbors()) {
          final SendInfo si = network.sendInfo.get(n);
          neighborStats.put(n, si != null ? Math.max(si.contentBitsReceived / si.time, si.contentBitsReceived / si.time) : 0L);
        }
        final NodeAddress slowNeighbor = Utils.findMinValueKey(neighborStats);
        removeNeighbor(slowNeighbor);
        network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), network.getAddress(), slowNeighbor));
        network.sendInfo.remove(slowNeighbor);
      }
    }
    refillNeighbors();
  }
  
  private void refillNeighbors() {
    final Set<NodeAddress> candidates = new TreeSet<NodeAddress>(mCache.keySet());
    candidates.removeAll(getNeighbors());
    for (int i = getNeighbors().size(); i < M && !candidates.isEmpty(); i++) {
      final NodeAddress candidate = Utils.pickRandomElement(candidates, r);
      candidates.remove(candidate);
      if (!network.isUp(candidate)) {
        i--;
        continue;
      }
      network.send(new ConnectionRequestMessage<Sizeable>(getMessageTag(), network.getAddress(), candidate));
    }
  }
  
  private void updatemCache() {
    final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress m : mCache.keySet()) {
      if (!mCache.get(m).updateTTL()) {
        toRemove.add(m);
      }
    }
    mCache.keySet().removeAll(toRemove);
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    scampOverlay.setServerMode();
  }
  
  @Override public void reConnect() {
    TextLogger.log(network.getAddress(), "reconnecting\n");
    final Set<NodeAddress> toRemove = new TreeSet<NodeAddress>(neighborNodes.keySet());
    DRB.removeNeighbors(toRemove);
  }
}

class memberInfo implements Serializable, Sizeable {
  private static final long serialVersionUID = -9073011804636406523L;
  public final long seq_num;
  public final int num_partner;
  public long time_to_live;
  public long last_update_time;
  
  @Override public long getSimulatedSize() {
    return Long.SIZE * 3 + Integer.SIZE;
  }
  
  public memberInfo(final MembershipMessage msg) {
    seq_num = msg.seq_num;
    num_partner = msg.num_partner;
    time_to_live = msg.time_to_live;
    last_update_time = Utils.getTime();
  }
  
  public boolean updateTTL() {
    time_to_live -= Utils.getTime() - last_update_time;
    last_update_time = Utils.getTime();
    return (time_to_live > 0);
  }
  
  @Override public String toString() {
    return seq_num + "," + num_partner;
  }
}

class partnerInfo {
  public long chunksSent;
  public long chunksReceived;
}
