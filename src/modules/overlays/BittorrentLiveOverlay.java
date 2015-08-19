package modules.overlays;

import ingredients.bootstrap.RandomGroupBootstrapIngredient;
import ingredients.overlay.BTliveNeighborAddIngredient;
import ingredients.overlay.ClubInfoIngredient;
import ingredients.overlay.RemoveParentsIngredient;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

import messages.BTLiveProtInfo;
import messages.Message;
import messages.SeedNodeMultipleTargetsReplyMessage;
import messages.SeedNodeRequestMessage;
import modules.P2PClient;
import utils.Common;
import experiment.frameworks.NodeAddress;

public class BittorrentLiveOverlay extends MultipleTreeOverlayModule<Object> {
  int totalClubs;
  int clubsForEachPeer;
  List<Integer> joinedClubs = new LinkedList<Integer>();
  final int minInClubDownload;
  final int maxInClubDownload;
  int minInClubUpload = 2;// TODO move to params
  int maxInClubUpload = 3;// TODO move to params
  static int initGroupSize = 10;
  static int minCandSize = 5;
  // tracker
  List<Integer> clubs = new LinkedList<Integer>();
  TreeSet<NodeAddress> banList = new TreeSet<NodeAddress>();
  P2PClient client;
  
  public BittorrentLiveOverlay(final P2PClient client, final int clubsForEachPeer, final int minInClubDownload,
      final int maxInClubDownload, final int parentDropTimeout, final Random r) {
    super(client, r);
    this.maxInClubDownload = maxInClubDownload;
    this.minInClubDownload = minInClubDownload;
    this.clubsForEachPeer = clubsForEachPeer;
    this.client = client;
    totalClubs = Common.currentConfiguration.descriptions;
    for (int i = 0; i < totalClubs; ++i) {
      addIngredient(new RemoveParentsIngredient(parentDropTimeout, i, banList, new Random(r.nextLong())), client);
    }
    addIngredient(new RandomGroupBootstrapIngredient(initGroupSize, new Random(r.nextLong())), client);
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    for (int i = 0; i < totalClubs; ++i) {
      joinedClubs.add(i);
    }
    for (int i = 0; i < totalClubs; ++i) {
      final ClubInfoIngredient cib = new ClubInfoIngredient(i, minCandSize, true, getDownloadGroupName(i), getUploadGroupName(i),
          banList, new Random(r.nextLong()));
      addIngredient(cib, client);
      final BTliveNeighborAddIngredient nab = new BTliveNeighborAddIngredient(i, cib, 0, 0, 2, 10/* TODO
                                                                                                  * maxupload
                                                                                                  * ? */, true, new Random(
          r.nextLong()));
      addIngredient(nab, client);
    }
  }
  
  @Override public void handleMessage(final Message message) {
    super.handleMessage(message);
    if (message instanceof SeedNodeRequestMessage) {
      if (knownNodes.contains(message.sourceId)) {
        return;
      }
      knownNodes.add(message.sourceId);
      if (clubs.size() < clubsForEachPeer) {
        clubs.clear();
        for (int i = 0; i < totalClubs; ++i) {
          clubs.add(i);
        }
        Collections.shuffle(clubs, r);
      }
      network.send(new BTLiveProtInfo(getMessageTag(), network.getAddress(), message.sourceId, totalClubs, new LinkedList<Integer>(
          clubs.subList(0, clubsForEachPeer))));
      clubs = clubs.subList(clubsForEachPeer, clubs.size());
    } else if (message instanceof BTLiveProtInfo) {
      totalClubs = ((BTLiveProtInfo) message).totalClubs;
      joinedClubs = ((BTLiveProtInfo) message).clubsToJoin;
      for (int i = 0; i < totalClubs; ++i) {
        final ClubInfoIngredient cib = new ClubInfoIngredient(i, minCandSize, joinedClubs.contains(i), getDownloadGroupName(i),
            getUploadGroupName(i), banList, new Random(r.nextLong()));
        addIngredient(cib, client);
        BTliveNeighborAddIngredient nab;
        if (joinedClubs.contains(i)) {
          nab = new BTliveNeighborAddIngredient(i, cib, minInClubDownload, maxInClubDownload, minInClubUpload, maxInClubUpload,
              true, new Random(r.nextLong()));
        } else {
          nab = new BTliveNeighborAddIngredient(i, cib, 1, 1, 0, 0, false, new Random(r.nextLong()));
          // one download connection for each other club
        }
        addIngredient(nab, client);
      }
    } else if (message instanceof SeedNodeMultipleTargetsReplyMessage) {
      knownNodes.addAll(((SeedNodeMultipleTargetsReplyMessage) message).targets);
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
  }
  
  @Override public void reConnect() {
    // don't reconnect!
    // super.reConnect();
  }
}
