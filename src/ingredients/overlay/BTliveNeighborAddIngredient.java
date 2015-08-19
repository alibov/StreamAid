package ingredients.overlay;

import ingredients.AbstractIngredient;
import ingredients.overlay.ClubInfoIngredient.CandidateInfo;
import interfaces.Sizeable;

import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ConnectionRequestApprovedMessage;
import messages.ConnectionRequestDeclinedMessage;
import messages.ConnectionRequestMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.MultipleTreeOverlayModule;
import utils.Utils;
import entites.SizeableBool;
import experiment.frameworks.NodeAddress;

public class BTliveNeighborAddIngredient extends AbstractIngredient<MultipleTreeOverlayModule<?>> {
  private final ClubInfoIngredient cib;
  private final int club;
  private final int maxUpload;
  private final int minUpload;
  // TODO no min upload handling
  private final int maxDownload;
  private final int minDownload;
  private final int connectionRequestDelay = 3;
  private int connectionRequestCurr = 1;
  private final int minCandsSize = 1;
  private final boolean joined;
  
  @Override public void setClientAndComponent(final P2PClient client, final MultipleTreeOverlayModule<?> alg) {
    super.setClientAndComponent(client, alg);
    Utils.checkExistence(alg.descriptorToSons, club, new TreeSet<NodeAddress>());
  }
  
  @Override public String getMessageTag() {
    return super.getMessageTag() + club;
  }
  
  public BTliveNeighborAddIngredient(final int club, final ClubInfoIngredient cib, final int minInClubDownload,
      final int maxInClubDownload, final int minInClubUpload, final int maxInClubUpload, final boolean joined, final Random r) {
    super(r);
    this.club = club;
    this.cib = cib;
    this.joined = joined;
    minDownload = minInClubDownload;
    maxDownload = maxInClubDownload;
    minUpload = minInClubUpload;
    maxUpload = maxInClubUpload;
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (connectionRequestCurr > 0) {
      connectionRequestCurr--;
    }
    if (connectionRequestCurr <= 0 && alg.getNodeGroup(cib.downloadGroupName).size() < minDownload) {
      addDownloadConnections(maxDownload - alg.getNodeGroup(cib.downloadGroupName).size());
    }
  }
  
  private void addDownloadConnections(final int toAdd) {
    if (cib.candInfo.size() < minCandsSize) {
      return;
    }
    connectionRequestCurr = connectionRequestDelay;
    // int totalUploads = 0;
    final TreeMap<Integer, TreeSet<NodeAddress>> uploads = new TreeMap<Integer, TreeSet<NodeAddress>>();
    for (final Entry<NodeAddress, CandidateInfo> entry : cib.candInfo.entrySet()) {
      if (alg.getNodeGroup(cib.downloadGroupName).contains(entry.getKey())
          || alg.getNodeGroup(cib.uploadGroupName).contains(entry.getKey())) {
        continue;
      }
      Utils.checkExistence(uploads, entry.getValue().uploadConnections, new TreeSet<NodeAddress>());
      // totalUploads += entry.getValue().uploadConnections;
      uploads.get(entry.getValue().uploadConnections).add(entry.getKey());
    }
    int added = 0;
    outer: for (final Integer ups : uploads.keySet()) {
      for (final NodeAddress cand : uploads.get(ups)) {
        client.network.send(new ConnectionRequestMessage<SizeableBool>(getMessageTag(), client.network.getAddress(), cand,
            new SizeableBool(joined)));
        added++;
        if (added >= toAdd) {
          break outer;
        }
      }
    }
  }
  
  @Override public void handleMessage(final Message message) {
    if (message instanceof ConnectionRequestMessage<?>) {
      if (!client.network.isUp(message.sourceId)) {
        return;
      }
      // overlay.addToGroup(GroupedOverlayAlgorithm.sonsGroupName,
      // message.sourceId);
      alg.descriptorToSons.get(club).add(message.sourceId);
      // TODO non joined download connections will never be dropped
      client.network.send(new ConnectionRequestApprovedMessage<Sizeable>(getMessageTag(), client.network.getAddress(),
          message.sourceId));
      if (((SizeableBool) ((ConnectionRequestMessage<?>) message).payload).l) {
        alg.addToGroup(cib.uploadGroupName, message.sourceId);
        if (alg.getNodeGroup(cib.uploadGroupName).size() > maxUpload) {
          dropUploadConnection();
        }
      }
    } else if (message instanceof ConnectionRequestApprovedMessage<?>) {
      alg.addToGroup(cib.downloadGroupName, message.sourceId);
      // overlay.addToGroup(GroupedOverlayAlgorithm.sonsGroupName,
      // message.sourceId);
    } else if (message instanceof ConnectionRequestDeclinedMessage) {
      alg.removeFromGroup(message.sourceId, cib.downloadGroupName);
      alg.removeFromGroup(message.sourceId, cib.uploadGroupName);
      for (final Set<NodeAddress> sons : alg.descriptorToSons.values()) {
        sons.remove(message.sourceId);
      }
    }
  }
  
  private void dropUploadConnection() {
    int drop = r.nextInt(alg.getNodeGroup(cib.uploadGroupName).size());
    NodeAddress toDrop = null;
    for (final NodeAddress curr : alg.getNodeGroup(cib.uploadGroupName)) {
      toDrop = curr;
      drop--;
      if (drop < 0) {
        break;
      }
    }
    alg.descriptorToSons.get(club).remove(toDrop);
    alg.removeFromGroup(toDrop, cib.uploadGroupName);
    client.network.send(new ConnectionRequestDeclinedMessage(getMessageTag(), client.network.getAddress(), toDrop));
  }
}
