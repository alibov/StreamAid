package modules.streaming.pullAlgorithms;

import ingredients.overlay.NeighborChunkAvailabilityIngredient.OperationMode;
import ingredients.streaming.mandatory.EarliestContinuousChunkVSInitIngredient;
import ingredients.streaming.mandatory.HandleChunkRequestsSentAscendingIngredient;
import ingredients.streaming.mandatory.PulseChunkRequestIngredient;

import java.util.Random;

import modules.P2PClient;
import modules.overlays.GroupedOverlayModule;

public class Pulse extends PullAlgorithm {
  public Pulse(final P2PClient client, final GroupedOverlayModule<?> overlay, final int requestFromNodeLimit,
      final boolean sourcePush, final int maxOffset, final int maxInitLatency, final Random r) {
    super(client, overlay, OperationMode.updateEveryRound, sourcePush, r);
    addIngredient(new EarliestContinuousChunkVSInitIngredient(maxOffset, maxInitLatency, new Random(r.nextLong())), client);
    addIngredient(new HandleChunkRequestsSentAscendingIngredient(new Random(r.nextLong())), client);
    addIngredient(new PulseChunkRequestIngredient(requestFromNodeLimit, new Random(r.nextLong())), client);
  }
}
