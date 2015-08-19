package modules.streaming.pullAlgorithms;

import ingredients.overlay.NeighborChunkAvailabilityIngredient;
import ingredients.overlay.NeighborChunkAvailabilityIngredient.OperationMode;
import ingredients.streaming.optional.SourcePushIngredient;

import java.util.Random;
import java.util.Set;

import modules.P2PClient;
import modules.overlays.OverlayModule;
import modules.streaming.StreamingModule;

public class PullAlgorithm extends StreamingModule {
  NeighborChunkAvailabilityIngredient NCAB;
  
  public PullAlgorithm(final P2PClient client, final OverlayModule<?> overlay, final Random r) {
    super(client, overlay, r);
  }
  
  public PullAlgorithm(final P2PClient client, final OverlayModule<?> overlay, final OperationMode opMode,
      final boolean sourcePush, final Random r) {
    super(client, overlay, r);
    NCAB = (NeighborChunkAvailabilityIngredient) overlay.getIngredient(NeighborChunkAvailabilityIngredient.class);
    if (NCAB == null) {
      NCAB = new NeighborChunkAvailabilityIngredient(opMode, !sourcePush, new Random(r.nextLong()));
      overlay.addIngredient(NCAB, client);
    }
    NCAB.setOperationMode(opMode);
    if (sourcePush) {
      addIngredient(new SourcePushIngredient(new Random(r.nextLong())), client);
    }
  }
  
  public Set<Long> getMissingChunks() {
    return client.player.getVs().getMissingChunks(fromChunk, latestChunk);
  }
  
  @Override public void deactivate() {
    super.deactivate();
    if (NCAB != null) {
      NCAB.setOperationMode(OperationMode.none);
    }
  }
}
