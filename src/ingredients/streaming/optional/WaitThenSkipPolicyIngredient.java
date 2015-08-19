package ingredients.streaming.optional;

import ingredients.AbstractIngredient;
import interfaces.NodeConnectionAlgorithm;

import java.util.Random;

import messages.Message;
import modules.P2PClient;
import modules.player.SkipPolicy;
import modules.player.VideoStream;

public class WaitThenSkipPolicyIngredient extends AbstractIngredient<NodeConnectionAlgorithm> {
  final int waitPeriod;
  
  public WaitThenSkipPolicyIngredient(final int waitPeriod, final Random r) {
    super(r);
    this.waitPeriod = waitPeriod;
  }
  
  @Override public void setClientAndComponent(final P2PClient client, final NodeConnectionAlgorithm alg) {
    super.setClientAndComponent(client, alg);
    if (client.player.waitIfNoChunk) {
      throw new RuntimeException("can't set skip policy when waitIfNoChunk is active");
    }
    client.player.setVSskipPolicy(new SkipPolicy() {
      private int secondsWaited = 0;
      
      @Override public boolean skipChunk(final VideoStream vs) {
        if (secondsWaited < waitPeriod) {
          secondsWaited++;
          return false;
        }
        secondsWaited = 0;
        return true;
      }
    });
  }
  
  @Override public void handleMessage(final Message message) {
    // no messages
  }
}
