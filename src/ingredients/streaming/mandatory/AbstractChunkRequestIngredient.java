package ingredients.streaming.mandatory;

import ingredients.AbstractIngredient;
import ingredients.overlay.NeighborChunkAvailabilityIngredient;

import java.util.Random;

import modules.P2PClient;
import modules.streaming.pullAlgorithms.PullAlgorithm;

abstract public class AbstractChunkRequestIngredient extends AbstractIngredient<PullAlgorithm> {
  protected NeighborChunkAvailabilityIngredient NCAB;
  private boolean requestMissingChunks = true;
  
  public AbstractChunkRequestIngredient(final Random r) {
    super(r);
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final PullAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    NCAB = (NeighborChunkAvailabilityIngredient) alg.mainOverlay.getIngredient(NeighborChunkAvailabilityIngredient.class);
    if (NCAB == null) {
      throw new RuntimeException("must have NeighborChunkAvailabilityBehavior before initializing " + getClass().getSimpleName());
    }
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    if (!client.isServerMode() && !NCAB.neighborAvailability.isEmpty() && requestMissingChunks) {
      requestMissingChunks();
    }
  }
  
  public void setRequestMissingChunks(final boolean requestMissingChunks) {
    this.requestMissingChunks = requestMissingChunks;
  }
  
  abstract protected void requestMissingChunks();
}
