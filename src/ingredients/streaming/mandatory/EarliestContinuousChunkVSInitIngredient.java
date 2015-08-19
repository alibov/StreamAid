package ingredients.streaming.mandatory;

import ingredients.AbstractIngredient;
import ingredients.overlay.NeighborChunkAvailabilityIngredient;

import java.util.Random;

import messages.Message;
import modules.P2PClient;
import modules.streaming.StreamingModule;
import utils.Utils;
import entites.Availability;

public class EarliestContinuousChunkVSInitIngredient extends AbstractIngredient<StreamingModule> {
  NeighborChunkAvailabilityIngredient NCAB;
  final private int maxOffset;
  final private int maxLatency;
  
  public EarliestContinuousChunkVSInitIngredient(final int maxOffset, final int maxLatency, final Random r) {
    super(r);
    this.maxOffset = maxOffset;
    this.maxLatency = maxLatency;
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final StreamingModule alg) {
    super.setClientAndComponent(client, alg);
    NCAB = (NeighborChunkAvailabilityIngredient) alg.mainOverlay.getIngredient(NeighborChunkAvailabilityIngredient.class);
    if (NCAB == null) {
      throw new RuntimeException(
          "must have NeighborChunkAvailabilityBehavior before initializing EarliestContinuousChunkVSInitBehavior");
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!NCAB.neighborAvailability.isEmpty() && !client.isServerMode()) {
      if (client.player.getVs() == null) {
        long maxChunk = Long.MIN_VALUE;
        for (final Availability avail : NCAB.neighborAvailability.values()) {
          final long currMaxChunk = avail.getEarliestContinuousChunk(maxOffset);
          if (currMaxChunk > maxChunk) {
            maxChunk = currMaxChunk;
          }
        }
        client.player.initVS(Math.max(maxChunk, Utils.getMovieTime() / 1000 - maxLatency));
      }
    }
  }
  
  @Override public void handleMessage(final Message message) {
    // do nothing
  }
}
