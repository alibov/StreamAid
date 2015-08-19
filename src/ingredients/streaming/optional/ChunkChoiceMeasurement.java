package ingredients.streaming.optional;

import ingredients.AbstractIngredient;
import ingredients.overlay.NeighborChunkAvailabilityIngredient;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import messages.Message;
import modules.P2PClient;
import modules.streaming.pullAlgorithms.PullAlgorithm;
import utils.Utils;
import entites.Availability;
import experiment.frameworks.NodeAddress;

public class ChunkChoiceMeasurement extends AbstractIngredient<PullAlgorithm> {
  public static Map<Integer, Integer> chunkChoice = new TreeMap<Integer, Integer>();
  private NeighborChunkAvailabilityIngredient NCAB;
  
  public ChunkChoiceMeasurement() {
    super(null);
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final PullAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    NCAB = (NeighborChunkAvailabilityIngredient) alg.mainOverlay.getIngredient(NeighborChunkAvailabilityIngredient.class);
    if (NCAB == null) {
      throw new RuntimeException("must have NeighborChunkAvailabilityBehavior before initializing " + getClass().getSimpleName());
    }
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!client.isServerMode() && !NCAB.neighborAvailability.isEmpty()) {
      populateChunkChoice();
    }
  }
  
  protected void populateChunkChoice() {
    final Set<Long> missing = alg.getMissingChunks();
    final Map<Long, Set<NodeAddress>> segmentToNodes = new TreeMap<Long, Set<NodeAddress>>();
    for (final Long i : missing) {
      segmentToNodes.put(i, new TreeSet<NodeAddress>());
      for (final Entry<NodeAddress, Availability> j : NCAB.neighborAvailability.entrySet()) {
        if (!client.network.isUp(j.getKey())) {
          continue;
        }
        if (j.getValue().hasChunk(i)) {
          segmentToNodes.get(i).add(j.getKey());
        }
      }
      final int n = segmentToNodes.get(i).size();
      if (n > 0) {
        Utils.checkExistence(chunkChoice, n, 0);
        chunkChoice.put(n, chunkChoice.get(n) + 1);
      }
    }
  }
}
