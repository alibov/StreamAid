package modules.streaming;

import java.util.Random;

import messages.Message;
import modules.P2PClient;
import modules.streaming.pullAlgorithms.PullAlgorithm;

public class PrimaryFallbackStreaming extends StreamingModule {
  private final PullAlgorithm fallbackAlg;
  private final StreamingModule mainAlg;
  
  public PrimaryFallbackStreaming(final P2PClient client, final StreamingModule mainAlg, final PullAlgorithm fallbackAlg,
      final Random r) {
    super(client, mainAlg.mainOverlay, r);
    this.mainAlg = mainAlg;
    this.fallbackAlg = fallbackAlg;
  }
  
  @Override public void handleMessage(final Message message) {
    throw new RuntimeException("PrimaryFallbackStreaming should not receive messages");
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    mainAlg.nextCycle();
    // if no push connection - request chunks in fallbackAlg.
    // stop requesting if main overlay reconnected
    // for (final AbstractBehavior b :
    // fallbackAlg.getBehaviors(AbstractChunkRequestBehavior.class)) {
    // ((AbstractChunkRequestBehavior)
    // b).setRequestMissingChunks(!mainAlg.isOverlayConnected());
    // } TODO fix me
    fallbackAlg.nextCycle();
  }
}
