package ingredients.streaming.mandatory;

import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.ChunkRequestMessage;
import messages.Message;
import modules.P2PClient;
import modules.overlays.GroupedOverlayModule;
import modules.overlays.PulseOverlay;
import modules.streaming.pullAlgorithms.PullAlgorithm;
import utils.Utils;
import entites.Availability;
import experiment.frameworks.NodeAddress;

public class PulseChunkRequestIngredient extends AbstractChunkRequestIngredient {
  private final int requestFromNodeLimit;
  GroupedOverlayModule<?> groupedOverlay;
  
  public PulseChunkRequestIngredient(final int requestFromNodeLimit, final Random r) {
    super(r);
    this.requestFromNodeLimit = requestFromNodeLimit;
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final PullAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    groupedOverlay = (GroupedOverlayModule<?>) alg.mainOverlay;
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
  
  @Override protected void requestMissingChunks() {
    final Set<Long> missing = alg.getMissingChunks();
    final Map<NodeAddress, ChunkRequestMessage> requests = new TreeMap<NodeAddress, ChunkRequestMessage>();
    final Map<Long, Set<NodeAddress>> segmentToNodes = new TreeMap<Long, Set<NodeAddress>>();
    final Map<Integer, Set<Long>> dupSet = new TreeMap<Integer, Set<Long>>();
    for (final Long i : missing) {
      segmentToNodes.put(i, new TreeSet<NodeAddress>());
      for (final Entry<NodeAddress, Availability> j : NCAB.neighborAvailability.entrySet()) {
        if (j.getValue().hasChunk(i)) {
          segmentToNodes.get(i).add(j.getKey());
        }
      }
      final int n = segmentToNodes.get(i).size();
      if (n == 1) {
        final NodeAddress chosenNode = segmentToNodes.get(i).iterator().next();
        addRequest(requests, i, chosenNode);
      } else if (n > 1) {
        Utils.checkExistence(dupSet, n, new TreeSet<Long>());
        dupSet.get(n).add(i);
      }
    }
    for (int n = 2; n <= NCAB.neighborAvailability.size(); n++) {
      if (!dupSet.containsKey(n)) {
        continue;
      }
      dupset: for (final Long i : dupSet.get(n)) {
        Set<NodeAddress> missingSet = new HashSet<NodeAddress>(segmentToNodes.get(i));
        missingSet.retainAll(groupedOverlay.getNodeGroup(PulseOverlay.missingGroupName));
        // first asking from Missing group
        for (final NodeAddress potentialNode : missingSet) {
          if (addRequest(requests, i, potentialNode)) {
            continue dupset;
          }
        }
        missingSet = new HashSet<NodeAddress>(segmentToNodes.get(i));
        missingSet.removeAll(groupedOverlay.getNodeGroup(PulseOverlay.missingGroupName));
        // if no Missing, then, the rest
        for (final NodeAddress potentialNode : missingSet) {
          if (addRequest(requests, i, potentialNode)) {
            continue dupset;
          }
        }
        // if we get here, no possible neighbor detected to request the chunk
        // from
      }
    }
    for (final ChunkRequestMessage req : requests.values()) {
      client.network.send(req);
    }
  }
  
  private boolean addRequest(final Map<NodeAddress, ChunkRequestMessage> requests, final Long i, final NodeAddress chosenNode) {
    Utils.checkExistence(requests, chosenNode,
        new ChunkRequestMessage(alg.getMessageTag(), client.network.getAddress(), chosenNode));
    if (requests.get(chosenNode).chunks.size() < requestFromNodeLimit) {
      requests.get(chosenNode).addRequestedChunk(i);
      return true;
    }
    return false;
  }
}
