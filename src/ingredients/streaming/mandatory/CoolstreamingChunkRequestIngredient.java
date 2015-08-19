package ingredients.streaming.mandatory;

import ingredients.overlay.InformationExchange.ExchangeType;
import ingredients.overlay.InformationExchange.InfoType;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkRequestMessage;
import messages.Message;
import modules.P2PClient;
import modules.streaming.pullAlgorithms.PullAlgorithm;
import utils.Common;
import utils.Utils;
import entites.Availability;
import experiment.frameworks.NodeAddress;

public class CoolstreamingChunkRequestIngredient extends AbstractChunkRequestIngredient {
  public CoolstreamingChunkRequestIngredient(final Random r) {
    super(r);
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final PullAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    alg.mainOverlay.infoExchange.requestInfoExchange(InfoType.UPLOADBANDWIDTHPERNEIGHBOR, ExchangeType.ONUPDATE);
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
  
  @Override protected void requestMissingChunks() {
    final Set<Long> missing = alg.getMissingChunks();
    final Map<NodeAddress, ChunkRequestMessage> requests = new TreeMap<NodeAddress, ChunkRequestMessage>();
    final Map<NodeAddress/* neighbor */, Map<Long/* chunk */, Long/* available
                                                                   * time */>> T = new TreeMap<NodeAddress, Map<Long, Long>>();
    final Map<Long, Set<NodeAddress>> segmentToNodes = new TreeMap<Long, Set<NodeAddress>>();
    final Map<Integer, Set<Long>> dupSet = new TreeMap<Integer, Set<Long>>();
    for (final Long i : missing) {
      segmentToNodes.put(i, new TreeSet<NodeAddress>());
      for (final Entry<NodeAddress, Availability> j : NCAB.neighborAvailability.entrySet()) {
        if (!client.network.isUp(j.getKey())) {
          continue;
        }
        Utils.checkExistence(T, j.getKey(), new TreeMap<Long, Long>());
        T.get(j.getKey()).put(i, client.player.getVs().getAvailableTime(i));
        if (j.getValue().hasChunk(i)) {
          segmentToNodes.get(i).add(j.getKey());
        }
      }
      final int n = segmentToNodes.get(i).size();
      if (n == 1) {
        final NodeAddress chosenNode = segmentToNodes.get(i).iterator().next();
        addRequest(missing, requests, T, i, chosenNode, getUploadBandwidth(chosenNode));
      } else if (n > 1) {
        Utils.checkExistence(dupSet, n, new TreeSet<Long>());
        dupSet.get(n).add(i);
      }
    }
    for (int n = 2; n <= NCAB.neighborAvailability.size(); n++) {
      if (!dupSet.containsKey(n)) {
        continue;
      }
      for (final Long i : dupSet.get(n)) {
        NodeAddress chosenNode = null;
        Long chosenNodeBandwidth = 0L;
        for (final NodeAddress potentialNode : segmentToNodes.get(i)) {
          final Long neighborBandwidth = getUploadBandwidth(potentialNode);
          if (T.get(potentialNode).get(i) <= Common.currentConfiguration.bitRate / neighborBandwidth) {
            continue;
          }
          if (neighborBandwidth > chosenNodeBandwidth) {
            chosenNodeBandwidth = neighborBandwidth;
            chosenNode = potentialNode;
          }
        }
        if (chosenNode != null) {
          addRequest(missing, requests, T, i, chosenNode, chosenNodeBandwidth);
        }
      }
    }
    for (final ChunkRequestMessage req : requests.values()) {
      client.network.send(req);
    }
  }
  
  private Long getUploadBandwidth(final NodeAddress potentialNode) {
    Long neighborBandwidth = (Long) alg.mainOverlay.infoExchange.getInfo(potentialNode, InfoType.UPLOADBANDWIDTHPERNEIGHBOR);
    if (neighborBandwidth == null) {
      int size = alg.mainOverlay.getNeighbors().size();
      if (size == 0) {
        size++;
      }
      neighborBandwidth = client.network.getUploadBandwidth() / size;
      // no data received from neighbor yet - approximate
    }
    return neighborBandwidth;
  }
  
  private void addRequest(final Set<Long> missing, final Map<NodeAddress, ChunkRequestMessage> requests,
      final Map<NodeAddress, Map<Long, Long>> T, final Long i, final NodeAddress chosenNode, final long chosenNodeBandwidth) {
    Utils.checkExistence(requests, chosenNode,
        new ChunkRequestMessage(alg.getMessageTag(), client.network.getAddress(), chosenNode));
    requests.get(chosenNode).addRequestedChunk(i);
    for (final Long j : missing) {
      if (j <= i) {
        continue;
      }
      Utils.checkExistence(T.get(chosenNode), j, client.player.getVs().getAvailableTime(j));
      // final long neighborBandwidth =
      // neighborUploadBandwidth.containsKey(chosenNode) ?
      // neighborUploadBandwidth.get(chosenNode)
      // : client.network.getUploadBandwidth() /
      // (pullAlg.mainOverlay.getNeighbors().size() + 1);
      T.get(chosenNode).put(j, T.get(chosenNode).get(j) - 1000 * Common.currentConfiguration.bitRate / chosenNodeBandwidth);
      // TODO need to know bandwidth between pair of nodes!
    }
  }
}
