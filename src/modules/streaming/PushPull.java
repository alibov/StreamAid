package modules.streaming;

import java.util.Random;

import messages.Message;
import modules.P2PClient;
import modules.overlays.OverlayModule;
import modules.overlays.TreeOverlayModule;
import modules.streaming.pullAlgorithms.PullAlgorithm;

public class PushPull extends StreamingModule {
  public PushTree pushAlg;
  public PullAlgorithm pullAlg;
  int disconnectedRounds = 0;
  
  public PushPull(final P2PClient client, final TreeOverlayModule<?> pushOverlay, final PullAlgorithm pullAlg, final Random r) {
    super(client, new OverlayModule<Object>(client, new Random(r.nextLong())) {
      // empty overlay
    }, r);
    pushAlg = new PushTree(client, pushOverlay, new Random(r.nextLong()));
    this.pullAlg = pullAlg;
  }
  
  @Override public void deactivate() {
    super.deactivate();
    pushAlg.deactivate();
    pullAlg.deactivate();
  }
  
  @Override public void setConfNumber(final int confNumber) {
    super.setConfNumber(confNumber);
    pushAlg.setConfNumber(confNumber);
    pullAlg.setConfNumber(confNumber);
  }
  
  @Override public void setEarliestChunk(final long chunk) {
    super.setEarliestChunk(chunk);
    pushAlg.setEarliestChunk(chunk);
    pullAlg.setEarliestChunk(chunk);
  }
  
  @Override public void setLatestChunk(final long chunk) {
    super.setLatestChunk(chunk);
    pushAlg.setLatestChunk(chunk);
    pullAlg.setLatestChunk(chunk);
  }
  
  @Override public void nextCycle() {
    super.nextCycle();
    pushAlg.nextCycle();
    pullAlg.latestChunk = (calculateMaxChunkToRequest());
    pullAlg.nextCycle();
  }
  
  @Override public void handleMessage(final Message message) {
    throw new RuntimeException("PushPullAlgorithm should not receive messages");
  }
  
  protected long calculateMaxChunkToRequest() {
    // don't request until connected at least once to the tree
    if (pushAlg.lastReceivedChunkIndex == -1) {
      return -1;
    }
    if (pushAlg.mainOverlay.isOverlayConnected()) {
      disconnectedRounds = 0;
    } else {
      disconnectedRounds++; // advance window if disconnected in push overlay
    }
    return pushAlg.lastReceivedChunkIndex + disconnectedRounds;
  }
  
  @Override public void setServerMode() {
    super.setServerMode();
    pushAlg.setServerMode();
    pullAlg.setServerMode();
  }
}
