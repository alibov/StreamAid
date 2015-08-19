package ingredients.streaming.optional;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import messages.Message;
import modules.P2PClient;
import modules.player.SkipPolicy;
import modules.player.VideoStream;
import entites.Bitmap;

public class LookaheadSkipPolicyIngredient extends AbstractIngredient<NodeConnectionAlgorithm> {
  final int numberOfLookaheadChunks;
  
  public LookaheadSkipPolicyIngredient(final int numberOfLookaheadChunks, final Random r) {
    super(r);
    this.numberOfLookaheadChunks = numberOfLookaheadChunks;
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final NodeConnectionAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    if (client.player.waitIfNoChunk) {
      throw new RuntimeException("can't set skip policy when waitIfNoChunk is active");
    }
    client.player.setVSskipPolicy(new SkipPolicy() {
      @Override public boolean skipChunk(final VideoStream vs) {
        final Bitmap bm = vs.getBitmap(vs.windowOffset);
        for (int i = 0; i < numberOfLookaheadChunks; i++) {
          if (!bm.hasChunk(vs.windowOffset + i + 1, client.player.waitIfNoChunk)) {
            return false;
          }
        }
        return true;
      }
    });
  }
  
  @Override public void handleMessage(final Message message) {
    // no messages
  }
}
