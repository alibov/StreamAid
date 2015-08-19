package ingredients.overlay;

import ingredients.AbstractIngredient;

import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.BTLivePong;
import messages.KnownNodesInClubRequest;
import messages.KnownNodesInClubResponse;
import messages.Message;
import modules.overlays.GroupedOverlayModule;
import experiment.frameworks.NodeAddress;

public class ClubInfoIngredient extends AbstractIngredient<GroupedOverlayModule<?>> {
  public class CandidateInfo {
    public CandidateInfo(final int download, final int upload) {
      downloadConnections = download;
      uploadConnections = upload;
    }
    
    int uploadConnections;
    int downloadConnections;
  }
  
  private final int club;
  static int reqTimeout = 3;
  private int currReqTimeout = 1;
  static int rePingTimeout = 10;
  private int currRePingTimeout = rePingTimeout;
  public final Map<NodeAddress, CandidateInfo> candInfo = new TreeMap<NodeAddress, ClubInfoIngredient.CandidateInfo>();
  public final Set<NodeAddress> pinged = new HashSet<NodeAddress>();
  private final int minSize;
  private final boolean joined;
  public final String downloadGroupName;
  public final String uploadGroupName;
  private final TreeSet<NodeAddress> banList;
  
  public ClubInfoIngredient(final int club, final int minSize, final boolean joined, final String downloadGroupName,
      final String uploadGroupName, final TreeSet<NodeAddress> banList, final Random r) {
    super(r);
    this.club = club;
    this.minSize = minSize;
    this.joined = joined;
    this.downloadGroupName = downloadGroupName;
    this.uploadGroupName = uploadGroupName;
    this.banList = banList;
  }
  
  @Override public String getMessageTag() {
    return super.getMessageTag() + club;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (currReqTimeout > 0) {
      currReqTimeout--;
    }
    currRePingTimeout--;
    if (currRePingTimeout == 0) {
      currRePingTimeout = rePingTimeout;
      pinged.clear();
      // pinged.addAll(banList);
    }
    removeDownNodes();
    candInfo.keySet().removeAll(banList);
    if (currReqTimeout <= 0 && candInfo.size() < minSize) {
      for (final NodeAddress n : alg.getKnownNodes()) {
        client.network.send(new KnownNodesInClubRequest(getMessageTag(), client.network.getAddress(), n));
        currReqTimeout = reqTimeout;
      }
    }
  }
  
  private void removeDownNodes() {
    final HashSet<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress n : candInfo.keySet()) {
      if (!client.network.isUp(n)) {
        toRemove.add(n);
      }
    }
    candInfo.keySet().removeAll(toRemove);
    pinged.removeAll(toRemove);
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof KnownNodesInClubRequest) {
      if (!client.network.isUp(message.sourceId)) {
        return;
      }
      final TreeSet<NodeAddress> cands = new TreeSet<NodeAddress>();
      cands.addAll(candInfo.keySet());
      if (joined) {
        cands.add(client.network.getAddress());
      }
      cands.remove(message.sourceId);
      if (!cands.isEmpty()) {
        client.network.send(new KnownNodesInClubResponse(getMessageTag(), client.network.getAddress(), message.sourceId, cands));
      }
    } else if (message instanceof KnownNodesInClubResponse) {
      for (final NodeAddress cand : ((KnownNodesInClubResponse) message).cands) {
        if (!pinged.contains(cand) && client.network.isUp(cand)) {
          client.network.send(new BTLivePing(getMessageTag(), client.network.getAddress(), cand));
          pinged.add(cand);
        }
      }
    } else if (message instanceof BTLivePing) {
      if (!client.network.isUp(message.sourceId)) {
        return;
      }
      client.network.send(new BTLivePong(getMessageTag(), client.network.getAddress(), message.sourceId, alg.getNodeGroup(
          downloadGroupName).size(), alg.getNodeGroup(uploadGroupName).size()));
    } else if (message instanceof BTLivePong) {
      candInfo.put(message.sourceId, new CandidateInfo(((BTLivePong) message).download, ((BTLivePong) message).upload));
    }
  }
}
