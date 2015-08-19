package modules.streaming.pullAlgorithms;

import ingredients.overlay.NeighborChunkAvailabilityIngredient.OperationMode;
import ingredients.streaming.mandatory.ChainsawChunkRequestIngredient;
import ingredients.streaming.mandatory.EarliestContinuousChunkVSInitIngredient;
import ingredients.streaming.mandatory.HandleChunkRequestsOnArrivalIngredient;

import java.util.Random;

import modules.P2PClient;
import modules.overlays.OverlayModule;

public class Chainsaw extends PullAlgorithm {
  public Chainsaw(final P2PClient client, final OverlayModule<?> overlay, final boolean sourcePush, final int maxOffset,
      final int maxInitLatency, final Random r) {
    super(client, overlay, OperationMode.UpdateEveryChunk, sourcePush, r);
    addIngredient(new HandleChunkRequestsOnArrivalIngredient(new Random(r.nextLong())), client);
    addIngredient(new EarliestContinuousChunkVSInitIngredient(maxOffset, maxInitLatency, new Random(r.nextLong())), client);
    addIngredient(new ChainsawChunkRequestIngredient(new Random(r.nextLong())), client);
  }
}
