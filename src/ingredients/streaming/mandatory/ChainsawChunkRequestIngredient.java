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
import utils.Utils;
import entites.Availability;
import experiment.frameworks.NodeAddress;

public class ChainsawChunkRequestIngredient extends AbstractChunkRequestIngredient {
  public ChainsawChunkRequestIngredient(final Random r) {
    super(r);
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
  
  @Override protected void requestMissingChunks() {
    final Map<NodeAddress, ChunkRequestMessage> requests = new TreeMap<NodeAddress, ChunkRequestMessage>();
    final Map<NodeAddress, Set<Long>> neighborToMissing = new TreeMap<NodeAddress, Set<Long>>();
    final Map<NodeAddress, Integer> neighborToMissingSize = new TreeMap<NodeAddress, Integer>();
    for (final Long i : alg.getMissingChunks()) {
      for (final Entry<NodeAddress, Availability> j : NCAB.neighborAvailability.entrySet()) {
        if (!client.network.isUp(j.getKey())) {
          continue;
        }
        if (j.getValue().hasChunk(i)) {
          Utils.checkExistence(neighborToMissing, j.getKey(), new TreeSet<Long>());
          neighborToMissing.get(j.getKey()).add(i);
        }
      }
    }
    updateSizeMap(neighborToMissing, neighborToMissingSize);
    while (!neighborToMissing.isEmpty()) {
      final NodeAddress neighbor = Utils.findMinValueKey(neighborToMissingSize);
      final long requestedChunk = Utils.pickRandomElement(neighborToMissing.get(neighbor), r);
      addRequest(neighbor, requests, requestedChunk);
      for (final Set<Long> missing : neighborToMissing.values()) {
        missing.remove(requestedChunk);
      }
      updateSizeMap(neighborToMissing, neighborToMissingSize);
    }
    for (final ChunkRequestMessage req : requests.values()) {
      client.network.send(req);
    }
  }
  
  private static void updateSizeMap(final Map<NodeAddress, Set<Long>> neighborToMissing,
      final Map<NodeAddress, Integer> neighborToMissingSize) {
    neighborToMissingSize.clear();
    final Set<NodeAddress> toRemove = new HashSet<NodeAddress>();
    for (final NodeAddress n : neighborToMissing.keySet()) {
      final int size = neighborToMissing.get(n).size();
      if (size > 0) {
        neighborToMissingSize.put(n, neighborToMissing.get(n).size());
      } else {
        toRemove.add(n);
      }
    }
    neighborToMissing.keySet().removeAll(toRemove);
  }
  
  private void addRequest(final NodeAddress chosenNode, final Map<NodeAddress, ChunkRequestMessage> requests, final Long i) {
    Utils.checkExistence(requests, chosenNode,
        new ChunkRequestMessage(alg.getMessageTag(), client.network.getAddress(), chosenNode));
    requests.get(chosenNode).addRequestedChunk(i);
  }
}
